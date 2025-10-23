package com.example.demoapp.upload;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FileUploadManager {
    private static final String TAG = "FileUploadManager";
    private static final int CHUNK_SIZE = 512 * 1024;
    private static final int MAX_CONCURRENT_UPLOADS = 6;
    private static final int MAX_RETRIES = 88888888;
    private static final String UPLOAD_REGISTER_URL = "http://120.53.248.2:65002/api/upload/register";
    private static final String UPLOAD_SAVE_URL = "http://120.53.248.2:65002/api/upload/save";
    private static final String UPLOAD_MERGE_URL = "http://120.53.248.2:65002/api/upload/merge";

    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface UploadCallback {
        void onProgress(int progress);
        void onSuccess(String url, String fileName);
        void onError(String error);
    }

    public FileUploadManager() {
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void uploadFile(Context context, Uri fileUri, String fileId, UploadCallback callback) {
        Log.d(TAG, "开始上传文件，fileId: " + fileId);
        executorService.execute(() -> {
            try {
                byte[] fileData = readFileData(context, fileUri);
                if (fileData == null) {
                    notifyError(callback, "无法读取文件");
                    return;
                }

                String fileName = getFileName(context, fileUri);
                long fileSize = fileData.length;
                int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
                Log.d(TAG, "文件: " + fileName + ", 大小: " + fileSize + ", 分片: " + totalChunks);

                // 诊断参数
                ParameterDiagnostics.diagnoseRegisterRequest(fileId, totalChunks, fileName, fileSize);

                // 1. 注册上传 - 无限重试
                while (!registerUpload(fileId, totalChunks, fileName, fileSize)) {
                    Thread.sleep(1000);
                }
                Log.d(TAG, "注册成功");

                // 2. 准备分片
                List<ChunkTask> tasks = new ArrayList<>();
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * CHUNK_SIZE;
                    int end = Math.min(start + CHUNK_SIZE, fileData.length);
                    byte[] chunk = new byte[end - start];
                    System.arraycopy(fileData, start, chunk, 0, chunk.length);
                    tasks.add(new ChunkTask(fileId, i, chunk, totalChunks, fileName));
                }

                // 3. 并发上传分片
                AtomicInteger completedChunks = new AtomicInteger(0);
                AtomicInteger successfulChunks = new AtomicInteger(0);  // ✅ 新增：成功的分片数
                AtomicBoolean uploadComplete = new AtomicBoolean(false);
                AtomicBoolean hasError = new AtomicBoolean(false);
                AtomicReference<String> finalUrl = new AtomicReference<>("");
                AtomicReference<String> finalName = new AtomicReference<>("");
                Semaphore semaphore = new Semaphore(MAX_CONCURRENT_UPLOADS);
                CountDownLatch latch = new CountDownLatch(totalChunks);

                for (ChunkTask task : tasks) {
                    executorService.execute(() -> {
                        try {
                            semaphore.acquire();
                            
                            // ✅ 修复：只在有错误时提前退出，不因为 uploadComplete 退出
                            // 原因：服务端可能错误地返回 200，但实际需要所有分片
                            if (hasError.get()) {
                                Log.d(TAG, "分片 " + task.chunkIndex + " - 跳过（已有错误）");
                                return;
                            }

                            // 诊断分片参数
                            ParameterDiagnostics.diagnoseChunkRequest(
                                task.fileId, 
                                task.chunkIndex, 
                                task.totalChunks, 
                                task.chunk.length
                            );

                            UploadResult result = uploadChunkWithRetry(task);
                            
                            // ✅ 验证后端是否正常处理分片
                            if (result.code == 200) {
                                // 服务器返回 200：可能是完成信号，但需要验证 URL
                                Log.d(TAG, "分片 " + task.chunkIndex + " - 服务器返回 200");
                                Log.d(TAG, "分片 " + task.chunkIndex + " - URL: " + result.url);
                                
                                // ✅ 关键修复：只有当 URL 不为空时，才认为是真正的完成信号
                                if (result.url != null && !result.url.isEmpty()) {
                                    Log.d(TAG, "分片 " + task.chunkIndex + " - 确认为完成信号（URL 有效）");
                                    uploadComplete.set(true);
                                    finalUrl.set(result.url);
                                    finalName.set(result.fileName);
                                } else {
                                    Log.w(TAG, "分片 " + task.chunkIndex + " - 返回 200 但 URL 为空，视为普通成功");
                                }
                                successfulChunks.incrementAndGet();  // ✅ 计入成功
                            } else if (result.code > 0 && result.code < 300) {
                                // 分片上传成功（2xx 成功码，如 201）
                                Log.d(TAG, "分片 " + task.chunkIndex + " - 后端正常处理（code: " + result.code + "）");
                                successfulChunks.incrementAndGet();  // ✅ 计入成功
                            } else if (result.code == 0) {
                                // 上传失败（明确的失败）
                                Log.e(TAG, "分片 " + task.chunkIndex + " - 上传失败（code: 0）: " + result.msg);
                                hasError.set(true);
                            } else if (result.code < 0) {
                                // 未知错误（如网络错误、超时等）
                                Log.e(TAG, "分片 " + task.chunkIndex + " - 未知错误（code: " + result.code + "）");
                                hasError.set(true);
                            } else {
                                // 其他响应码（3xx, 4xx, 5xx）视为失败
                                Log.e(TAG, "分片 " + task.chunkIndex + " - 后端返回错误码（code: " + result.code + "）");
                                hasError.set(true);
                            }

                            int completed = completedChunks.incrementAndGet();
                            int progress = (int) ((completed * 100.0) / totalChunks);
                            notifyProgress(callback, progress);

                        } catch (Exception e) {
                            Log.e(TAG, "分片 " + task.chunkIndex + " - 上传异常", e);
                            hasError.set(true);
                        } finally {
                            semaphore.release();
                            latch.countDown();
                        }
                    });
                }

                latch.await();

                Log.d(TAG, "========== 所有分片任务完成 ==========");
                Log.d(TAG, "已完成任务数: " + completedChunks.get() + "/" + totalChunks);
                Log.d(TAG, "成功处理分片数: " + successfulChunks.get() + "/" + totalChunks);
                Log.d(TAG, "是否有错误: " + hasError.get());
                Log.d(TAG, "是否提前完成: " + uploadComplete.get());

                // ✅ 关键检查：确保所有分片都被后端正常处理
                if (hasError.get()) {
                    Log.e(TAG, "========== 上传失败 ==========");
                    Log.e(TAG, "原因：存在上传失败的分片");
                    notifyError(callback, "部分分片上传失败");
                    return;
                }

                if (successfulChunks.get() != totalChunks) {
                    Log.e(TAG, "========== 上传失败 ==========");
                    Log.e(TAG, "原因：成功分片数(" + successfulChunks.get() + ") != 总分片数(" + totalChunks + ")");
                    notifyError(callback, "部分分片未被后端正常处理");
                    return;
                }

                Log.d(TAG, "✅ 验证通过：所有 " + totalChunks + " 个分片都已被后端正常处理");

                // ✅ 修复：检查是否真正提前完成（必须有有效的 URL）
                if (uploadComplete.get() && finalUrl.get() != null && !finalUrl.get().isEmpty()) {
                    // 服务器真正完成了合并，有有效的 URL
                    Log.d(TAG, "========== 服务器提前完成（跳过 merge） ==========");
                    Log.d(TAG, "回调URL: " + finalUrl.get());
                    Log.d(TAG, "回调文件名: " + finalName.get());
                    notifySuccess(callback, finalUrl.get(), finalName.get());
                    return;
                } else if (uploadComplete.get()) {
                    // 服务器返回了 200 但 URL 为空，这不是真正的完成
                    Log.w(TAG, "========== 服务器返回 200 但 URL 为空 ==========");
                    Log.w(TAG, "说明：服务器响应异常，将继续调用 merge 接口");
                }

                // 4. 合并文件 - 无限重试
                Log.d(TAG, "========== 开始调用 merge 接口 ==========");
                Log.d(TAG, "原因：所有分片上传完成，需要合并");
                
                // 诊断合并参数
                ParameterDiagnostics.diagnoseMergeRequest(fileId);
                
                MergeResult mergeResult;
                int mergeAttempts = 0;
                while (true) {
                    mergeAttempts++;
                    Log.d(TAG, "Merge 尝试 #" + mergeAttempts);
                    
                    mergeResult = mergeChunks(fileId);
                    
                    if (mergeResult.code == 200) {
                        Log.d(TAG, "Merge 成功！");
                        break;
                    } else {
                        Log.e(TAG, "Merge 失败，code: " + mergeResult.code + "，1秒后重试...");
                    }
                    
                    Thread.sleep(1000);
                }

                Log.d(TAG, "========== 准备回调成功 ==========");
                Log.d(TAG, "回调URL: " + mergeResult.url);
                Log.d(TAG, "回调文件名: " + fileName);
                notifySuccess(callback, mergeResult.url, fileName);

            } catch (Exception e) {
                Log.e(TAG, "上传错误", e);
                notifyError(callback, e.getMessage());
            }
        });
    }

    private byte[] readFileData(Context context, Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            is.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "读取文件错误", e);
            return null;
        }
    }

    private String getFileName(Context context, Uri uri) {
        String fileName = "unknown_file";
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取文件名错误", e);
        }
        return fileName;
    }

    private boolean registerUpload(String fileId, int totalChunks, String fileName, long fileSize) {
        HttpURLConnection conn = null;
        try {
            Log.d(TAG, "========== 注册上传 ==========");
            Log.d(TAG, "请求URL: " + UPLOAD_REGISTER_URL);
            Log.d(TAG, "File-Id: " + fileId);
            Log.d(TAG, "Total-Chunks: " + totalChunks);
            Log.d(TAG, "File-Name: " + fileName);
            Log.d(TAG, "File-Size: " + fileSize);
            
            URL url = new URL(UPLOAD_REGISTER_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            
            // 完全对齐服务器端请求头处理
            // 所有必需的请求头
            conn.setRequestProperty("X-File-Id", fileId);
            conn.setRequestProperty("X-Total-Chunks", String.valueOf(totalChunks));
            conn.setRequestProperty("X-File-Name", URLEncoder.encode(fileName, "UTF-8"));
            conn.setRequestProperty("X-File-Size", String.valueOf(fileSize));
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "注册响应码: " + responseCode);
            
            // 对齐服务器端响应处理
            if (responseCode == 200) {
                String response = readResponse(conn);
                Log.d(TAG, "注册响应体: " + response);
                
                if (response == null || response.isEmpty()) {
                    Log.e(TAG, "注册响应为空");
                    return false;
                }
                
                JSONObject json = new JSONObject(response);
                int code = json.optInt("code", -1);
                String msg = json.optString("msg", "");
                Log.d(TAG, "解析结果 - code: " + code + ", msg: " + msg);
                
                return code == 200;
            } else {
                // 读取错误响应
                String errorResponse = readErrorResponse(conn);
                Log.e(TAG, "注册失败，响应码: " + responseCode + ", 错误: " + errorResponse);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "注册错误", e);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private UploadResult uploadChunkWithRetry(ChunkTask task) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(UPLOAD_SAVE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                
                // 完全对齐服务器端请求头处理
                // 所有必需的请求头
                conn.setRequestProperty("X-File-Id", task.fileId);
                conn.setRequestProperty("X-Chunk-Index", String.valueOf(task.chunkIndex));
                conn.setRequestProperty("X-Total-Chunks", String.valueOf(task.totalChunks));
                conn.setRequestProperty("X-Chunk-Size", String.valueOf(task.chunk.length));
                conn.setRequestProperty("Content-Type", "application/octet-stream");
                conn.setRequestProperty("Content-Length", String.valueOf(task.chunk.length));
                conn.setRequestProperty("Accept", "application/json");
                
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                // 写入分片数据
                OutputStream os = conn.getOutputStream();
                os.write(task.chunk);
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "分片 " + task.chunkIndex + " 响应码: " + responseCode);
                
                // 对齐服务器端响应处理
                if (responseCode == 200) {
                    String response = readResponse(conn);
                    Log.d(TAG, "分片 " + task.chunkIndex + " 响应体: " + response);
                    
                    if (response == null || response.isEmpty()) {
                        Log.e(TAG, "分片 " + task.chunkIndex + " 响应为空");
                        conn.disconnect();
                        continue;
                    }
                    
                    JSONObject json = new JSONObject(response);
                    int code = json.optInt("code", -1);
                    String msg = json.optString("msg", "");

                    Log.d(TAG, "========== 分片 " + task.chunkIndex + " 响应解析 ==========");
                    Log.d(TAG, "code: " + code);
                    Log.d(TAG, "msg: " + msg);

                    if (code == 200) {
                        // 服务器返回 200 表示文件上传完成（提前完成，无需 merge）
                        String fileName = json.optString("name", task.fileName);
                        String fileUrl = json.optString("url", "");
                        Log.d(TAG, "========== 服务器返回完成信号（code == 200） ==========");
                        Log.d(TAG, "url: " + fileUrl);
                        Log.d(TAG, "name: " + fileName);
                        Log.d(TAG, "说明：服务器已自动完成合并，无需调用 merge 接口");
                        conn.disconnect();
                        return new UploadResult(code, fileUrl, fileName, "");
                    } else if (code == 0) {
                        // 上传失败
                        Log.e(TAG, "========== 分片上传失败（code == 0） ==========");
                        Log.e(TAG, "msg: " + msg);
                        conn.disconnect();
                        return new UploadResult(code, "", "", msg);
                    } else if (code > 0 && code < 300) {
                        // 分片上传成功（2xx 成功码，如 201）
                        Log.d(TAG, "========== 分片上传成功（code == " + code + "） ==========");
                        Log.d(TAG, "说明：后端已正常处理此分片");
                        Log.d(TAG, "说明：所有分片上传完成后，将调用 merge 接口");
                        conn.disconnect();
                        return new UploadResult(code, "", "", "");
                    } else {
                        // 其他响应码视为失败
                        Log.e(TAG, "========== 分片上传失败（code == " + code + "） ==========");
                        Log.e(TAG, "msg: " + msg);
                        Log.e(TAG, "说明：后端返回了非成功的响应码");
                        conn.disconnect();
                        return new UploadResult(0, "", "", "后端返回非成功响应码: " + code);
                    }
                } else {
                    // 读取错误响应
                    String errorResponse = readErrorResponse(conn);
                    Log.e(TAG, "分片 " + task.chunkIndex + " 失败，响应码: " + responseCode + ", 错误: " + errorResponse);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "分片 " + task.chunkIndex + " 错误，重试 " + retries, e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            
            retries++;
            try {
                Thread.sleep(1000 * Math.min(retries, 5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new UploadResult(0, "", "", "达到最大重试次数");
    }

    private MergeResult mergeChunks(String fileId) {
        HttpURLConnection conn = null;
        try {
            Log.d(TAG, "========== 合并文件 ==========");
            Log.d(TAG, "请求URL: " + UPLOAD_MERGE_URL);
            Log.d(TAG, "File-Id: " + fileId);
            
            URL url = new URL(UPLOAD_MERGE_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            
            // 完全对齐服务器端请求头处理
            // 所有必需的请求头
            conn.setRequestProperty("X-File-Id", fileId);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            
            // 可能需要的额外请求头
            conn.setDoOutput(false);  // 合并请求不需要发送数据
            
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "合并响应码: " + responseCode);
            
            // 对齐服务器端响应处理
            if (responseCode == 200) {
                String response = readResponse(conn);
                Log.d(TAG, "合并响应体: " + response);
                
                if (response == null || response.isEmpty()) {
                    Log.e(TAG, "合并响应为空");
                    return new MergeResult(-1, "", "");
                }
                
                JSONObject json = new JSONObject(response);
                int code = json.optInt("code", -1);
                String msg = json.optString("msg", "");
                String fileUrl = json.optString("url", "");
                
                Log.d(TAG, "解析结果 - code: " + code + ", msg: " + msg + ", url: " + fileUrl);
                
                if (code == 200) {
                    Log.d(TAG, "========== 合并成功 ==========");
                    Log.d(TAG, "最终URL: " + fileUrl);
                    return new MergeResult(code, fileUrl, "");
                } else {
                    Log.e(TAG, "合并失败 - code: " + code + ", msg: " + msg);
                    return new MergeResult(code, "", "");
                }
            } else {
                // 读取错误响应
                String errorResponse = readErrorResponse(conn);
                Log.e(TAG, "合并失败，响应码: " + responseCode + ", 错误: " + errorResponse);
                return new MergeResult(-1, "", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "合并错误", e);
            return new MergeResult(-1, "", "");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection conn) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "读取响应错误", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, "关闭 reader 错误", e);
                }
            }
        }
    }
    
    private String readErrorResponse(HttpURLConnection conn) {
        BufferedReader reader = null;
        try {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream == null) {
                return "无错误详情";
            }
            reader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "读取错误响应错误", e);
            return "无法读取错误详情";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, "关闭 error reader 错误", e);
                }
            }
        }
    }

    private void notifyProgress(UploadCallback callback, int progress) {
        mainHandler.post(() -> callback.onProgress(progress));
    }

    private void notifySuccess(UploadCallback callback, String url, String fileName) {
        mainHandler.post(() -> callback.onSuccess(url, fileName));
    }

    private void notifyError(UploadCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private static class ChunkTask {
        String fileId;
        int chunkIndex;
        byte[] chunk;
        int totalChunks;
        String fileName;

        ChunkTask(String fileId, int chunkIndex, byte[] chunk, int totalChunks, String fileName) {
            this.fileId = fileId;
            this.chunkIndex = chunkIndex;
            this.chunk = chunk;
            this.totalChunks = totalChunks;
            this.fileName = fileName;
        }
    }

    private static class UploadResult {
        int code;
        String url;
        String fileName;
        String msg;

        UploadResult(int code, String url, String fileName, String msg) {
            this.code = code;
            this.url = url;
            this.fileName = fileName;
            this.msg = msg;
        }
    }

    private static class MergeResult {
        int code;
        String url;
        String fileName;

        MergeResult(int code, String url, String fileName) {
            this.code = code;
            this.url = url;
            this.fileName = fileName;
        }
    }
}
