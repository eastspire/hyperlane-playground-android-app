# 聊天功能实现说明

## 概述
已成功实现聊天界面的历史消息加载功能和 URL 超链接支持。

## 功能列表

### 1. 历史消息加载
- 首次进入自动加载最新的20条历史消息
- 滚动到顶部时自动加载更早的消息
- 使用 `beforeId` 进行分页
- 保持滚动位置不跳动
- 显示加载指示器

### 2. Markdown 渲染和媒体支持 ✨ 增强
- **Markdown 链接**：`[文本](url)` 格式的链接在浏览器中打开
- **纯文本 URL**：自动识别 http/https 链接并在浏览器中打开
- **图片展示**：Markdown 图片 `![alt](image_url)` 在 APP 内展示
  - 支持格式：jpg, jpeg, png, gif, webp, bmp
  - 点击图片全屏查看
  - 使用 Glide 加载，支持缓存
- **视频播放**：视频链接在 APP 内播放
  - 支持格式：mp4, webm, mkv, avi, mov, m3u8
  - 使用 ExoPlayer 播放
  - 支持横竖屏切换
- **IP 地址和端口号**：支持 `http://192.168.1.1:8080` 格式

### 3. 消息对齐方式
- 消息从顶部开始显示（移除了 stackFromEnd）
- 历史消息在上方，新消息在下方
- 符合传统聊天应用的显示习惯

## 修改的文件

### 1. build.gradle
**新增依赖**：
- Markwon 图片插件：`io.noties.markwon:image:4.6.2`
- Markwon Glide 插件：`io.noties.markwon:image-glide:4.6.2`
- Markwon HTML 插件：`io.noties.markwon:html:4.6.2`
- Markwon Linkify 插件：`io.noties.markwon:linkify:4.6.2`
- Glide 图片加载库：`com.github.bumptech.glide:glide:4.16.0`
- ExoPlayer 视频播放器：`androidx.media3:media3-exoplayer:1.2.0`
- ExoPlayer UI：`androidx.media3:media3-ui:1.2.0`

### 2. ChatAdapter.java
**增强功能：Markdown 渲染和媒体支持**

1. **Markwon 配置**：
   - `GlideImagesPlugin` - 图片加载和显示
   - `LinkifyPlugin` - 自动识别链接
   - 自定义 `LinkResolver` - 根据链接类型分发处理

2. **新增方法**：
   - `isImageUrl(String url)` - 判断是否为图片链接
   - `isVideoUrl(String url)` - 判断是否为视频链接
   - `openImageViewer(String url)` - 打开图片查看器
   - `openVideoPlayer(String url)` - 打开视频播放器
   - `openUrlInBrowser(String url)` - 在浏览器中打开链接

3. **链接处理逻辑**：
   - 图片链接 → ImageViewerActivity（APP 内查看）
   - 视频链接 → VideoPlayerActivity（APP 内播放）
   - 其他链接 → 系统浏览器

### 3. ImageViewerActivity.java（新建）
**图片查看器**：
- 使用 Glide 加载图片
- 支持缩放和平移
- 点击关闭
- 显示加载进度

### 4. VideoPlayerActivity.java（新建）
**视频播放器**：
- 使用 ExoPlayer 播放视频
- 支持多种视频格式
- 支持横竖屏切换
- 自动播放
- 错误处理

### 5. AndroidManifest.xml
**注册新 Activity**：
- ImageViewerActivity - 无 ActionBar 主题
- VideoPlayerActivity - 无 ActionBar，支持屏幕旋转

### ChatFragmentNew.java & ChatFragment.java
**历史消息加载 + 消息对齐调整**

1. 移除了 `layoutManager.setStackFromEnd(true)`
2. 消息从顶部开始显示
3. 实现了完整的历史加载逻辑

### ChatMessage.java
- 添加了 `id` 字段用于分页

### ChatApiService.java
- 新增 `getChatHistory()` API 方法
- 新增 `parseChatMessage()` 解析方法
- 新增 `HistoryCallback` 接口

### item_loading_history.xml（新建）
- 历史消息加载指示器的布局文件

## 使用示例

### 1. Markdown 链接（浏览器打开）
```markdown
[点击访问 Google](https://www.google.com)
[查看文档](http://example.com/docs)
[服务器地址](http://120.53.248.2:65002)
```

### 2. 纯文本 URL（浏览器打开）
```
访问 https://www.google.com 了解更多
查看 http://example.com 的内容
服务器地址：http://120.53.248.2:65002
本地测试：http://192.168.1.1:8080/api
```

### 3. Markdown 图片（APP 内展示）
```markdown
![示例图片](https://example.com/image.jpg)
![头像](http://120.53.248.2:65002/uploads/avatar.png)
```

支持的图片格式：
- JPG/JPEG
- PNG
- GIF
- WebP
- BMP

### 4. 视频链接（APP 内播放）
```markdown
[观看视频](https://example.com/video.mp4)
[直播流](http://120.53.248.2:65002/stream.m3u8)
```

支持的视频格式：
- MP4
- WebM
- MKV
- AVI
- MOV
- M3U8（HLS 直播流）

### 5. IP 地址和端口号
```
http://192.168.1.1:8080          → 浏览器打开
http://120.53.248.2:65002        → 浏览器打开
https://10.0.0.1:3000/api/test   → 浏览器打开
```

## 技术细节

### URL 正则表达式
支持两种格式的 URL：

1. **域名格式**：`https?://(?:[a-zA-Z0-9\-]+\.)*[a-zA-Z0-9\-]+(?::[0-9]+)?(?:/[^\s]*)?`
   - 示例：`http://example.com`, `https://www.google.com:8080/path`

2. **IP地址格式**：`https?://(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?::[0-9]+)?(?:/[^\s]*)?`
   - 示例：`http://192.168.1.1`, `http://120.53.248.2:65002/api/chat`

```java
String domainUrl = "https?://(?:[a-zA-Z0-9\\-]+\\.)*[a-zA-Z0-9\\-]+(?::[0-9]+)?(?:/[^\\s]*)?";
String ipUrl = "https?://(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]+)?(?:/[^\\s]*)?";
String urlRegex = "(" + domainUrl + "|" + ipUrl + ")";
Pattern urlPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
```

### ClickableSpan 实现
```java
ClickableSpan clickableSpan = new ClickableSpan() {
    @Override
    public void onClick(@NonNull View widget) {
        openUrlInBrowser(url);
    }
};
```

### Intent 打开浏览器
```java
Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
context.startActivity(intent);
```

## 注意事项

1. **链接安全性**：当前实现会打开所有 http/https 链接，建议在生产环境中添加 URL 白名单验证
2. **协议支持**：目前只支持 http 和 https 协议
3. **LinkMovementMethod**：必须设置 `LinkMovementMethod.getInstance()` 才能使链接可点击
4. **Spannable 处理**：需要正确处理 Markdown 渲染后的 Spannable 对象
5. **异常处理**：已添加 try-catch 防止无效 URL 导致应用崩溃

## 测试建议

1. 测试 Markdown 格式的链接
2. 测试纯文本 URL
3. 测试没有协议前缀的 URL
4. 测试无效的 URL（确保不会崩溃）
5. 测试长 URL 的显示和点击
6. 测试包含特殊字符的 URL
