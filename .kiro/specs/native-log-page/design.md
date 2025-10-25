# Design Document

## Overview

设计一个独立的 Native 日志查看页面（LogFragment），集成到现有的 ViewPager 架构中。日志页面将位于 Chat 页面左侧（索引 0），用户可以通过左右滑动在日志页面和其他页面之间切换。日志系统将记录整个应用的所有模块日志。

## Architecture

### 整体架构

```
MainActivityWithViewPager
    └── ViewPager2
        ├── LogFragment (Index 0) - 新增日志页面
        ├── ChatFragment (Index 1) - 原 Index 0
        ├── ExploreFragment (Index 2) - 原 Index 1
        ├── TraceFragment (Index 3) - 原 Index 2
        └── ProfileFragment (Index 4) - 原 Index 3
```

### 日志系统架构

```
NativeLogManager (Singleton)
    ├── 日志存储 (CopyOnWriteArrayList<NativeLog>)
    ├── 监听器管理 (List<LogListener>)
    └── 日志记录方法 (e/i/d)
        ↓
    各模块记录日志
        ├── ChatFragment
        ├── WebSocketManager
        ├── FileUploadManager
        ├── ChatApplication
        └── 其他模块
        ↓
    LogFragment 显示日志
        ├── 日志级别过滤
        ├── 时间逆序排序
        └── 实时更新
```

## Components and Interfaces

### 1. LogFragment

**职责**: 独立的日志查看页面

**主要组件**:
- RecyclerView: 显示日志列表
- RadioGroup: 日志级别选择器（ERROR/INFO/DEBUG）
- Button: 清除日志按钮
- NativeLogAdapter: 日志列表适配器

**接口实现**:
```java
public class LogFragment extends Fragment implements NativeLogManager.LogListener {
    private RecyclerView logRecyclerView;
    private NativeLogAdapter logAdapter;
    private RadioGroup logLevelGroup;
    private Button btnClearLogs;
    private NativeLog.Level currentLogLevel = NativeLog.Level.ERROR;
    
    @Override
    public void onLogAdded(NativeLog log);
}
```

### 2. ViewPagerAdapter (修改)

**修改内容**:
- 增加页面数量从 4 到 5
- 调整页面索引，LogFragment 为索引 0
- 其他页面索引依次后移

**修改后的映射**:
```java
position 0 -> LogFragment (新增)
position 1 -> ChatFragment (原 0)
position 2 -> ExploreFragment (原 1)
position 3 -> TraceFragment (原 2)
position 4 -> ProfileFragment (原 3)
```

### 3. NativeLogManager (已存在，无需修改)

**职责**: 单例日志管理器

**核心方法**:
```java
public void e(String tag, String message); // ERROR 日志
public void i(String tag, String message); // INFO 日志
public void d(String tag, String message); // DEBUG 日志
public List<NativeLog> getLogs(NativeLog.Level level); // 获取指定级别日志
public void addListener(LogListener listener); // 添加监听器
public void removeListener(LogListener listener); // 移除监听器
public void clearLogs(); // 清除所有日志
```

### 4. NativeLogAdapter (已存在，无需修改)

**职责**: RecyclerView 适配器，显示日志列表

**核心方法**:
```java
public void setLogs(List<NativeLog> logs); // 设置日志列表
public void addLog(NativeLog log); // 添加单条日志到顶部
```

### 5. ChatFragment (修改)

**修改内容**:
- 移除嵌入的日志面板 UI
- 移除日志面板相关代码
- 保留日志记录功能（调用 NativeLogManager）

## Data Models

### NativeLog (已存在)

```java
public class NativeLog {
    public enum Level {
        ERROR, INFO, DEBUG
    }
    
    private Level level;
    private String tag;
    private String message;
    private long timestamp;
}
```

## UI Layout Design

### LogFragment 布局 (fragment_log.xml)

```xml
<LinearLayout orientation="vertical">
    <!-- 顶部标题栏 -->
    <LinearLayout id="title_bar">
        <TextView text="Native Logs" />
    </LinearLayout>
    
    <!-- 日志级别选择器 -->
    <LinearLayout id="log_level_selector">
        <TextView text="日志级别：" />
        <RadioGroup id="log_level_group">
            <RadioButton id="radio_error" text="ERROR" checked="true" />
            <RadioButton id="radio_info" text="INFO" />
            <RadioButton id="radio_debug" text="DEBUG" />
        </RadioGroup>
    </LinearLayout>
    
    <!-- 日志列表 -->
    <RecyclerView id="log_recycler_view" />
    
    <!-- 清除按钮 -->
    <Button id="btn_clear_logs" text="清除日志" />
</LinearLayout>
```

### ChatFragment 布局修改 (fragment_chat.xml)

**移除内容**:
- log_panel (整个日志面板)
- btn_toggle_log (切换按钮)
- log_recycler_view (日志列表)
- log_level_group (级别选择器)
- btn_clear_logs (清除按钮)

**恢复内容**:
- chat_recycler_view 恢复为全宽布局
- 移除与 log_panel 的约束关系

## Page Navigation Flow

### 页面切换流程

```
LogFragment (0) <--左右滑动--> ChatFragment (1) <--左右滑动--> ExploreFragment (2) ...
```

### 默认启动页面

- 保持当前默认启动页面为 ExploreFragment (新索引 2)
- 或根据需要调整为 ChatFragment (新索引 1)

## Error Handling

### 日志系统错误处理

1. **日志记录失败**
   - 捕获异常，避免影响主业务逻辑
   - 输出到 Android Logcat 作为备份

2. **内存溢出**
   - 限制日志数量上限（1000条）
   - 自动删除最旧的日志

3. **UI 更新失败**
   - 使用 Handler 确保在主线程更新
   - 捕获 UI 更新异常

### Fragment 生命周期处理

1. **onViewCreated**
   - 初始化 UI 组件
   - 注册日志监听器
   - 加载初始日志

2. **onDestroyView**
   - 注销日志监听器
   - 清理资源

## Testing Strategy

### 单元测试

1. **NativeLogManager 测试**
   - 测试日志添加功能
   - 测试日志过滤功能
   - 测试日志数量限制
   - 测试监听器通知

2. **LogFragment 测试**
   - 测试日志级别切换
   - 测试清除日志功能
   - 测试日志实时更新

### 集成测试

1. **ViewPager 集成测试**
   - 测试页面滑动切换
   - 测试页面索引正确性
   - 测试页面状态保持

2. **日志系统集成测试**
   - 测试各模块日志记录
   - 测试日志在 LogFragment 中显示
   - 测试跨页面日志记录

### UI 测试

1. **页面切换测试**
   - 从 LogFragment 左滑到 ChatFragment
   - 从 ChatFragment 右滑到 LogFragment
   - 测试滑动动画流畅性

2. **日志显示测试**
   - 测试日志列表显示
   - 测试日志颜色区分
   - 测试日志时间格式

## Implementation Notes

### 实现顺序

1. **Phase 1: 创建 LogFragment**
   - 创建 LogFragment.java
   - 创建 fragment_log.xml 布局
   - 实现基本的日志显示功能

2. **Phase 2: 修改 ViewPagerAdapter**
   - 增加页面数量
   - 调整页面索引映射
   - 添加 LogFragment 到索引 0

3. **Phase 3: 清理 ChatFragment**
   - 移除嵌入的日志面板 UI
   - 移除日志面板相关代码
   - 恢复 chat_recycler_view 布局

4. **Phase 4: 更新布局文件**
   - 修改 fragment_chat.xml
   - 移除日志面板相关组件

5. **Phase 5: 测试和优化**
   - 测试页面切换
   - 测试日志记录和显示
   - 优化性能和用户体验

### 注意事项

1. **页面索引变化**
   - 所有引用页面索引的代码需要更新
   - MainActivity 中的 setCurrentItem 需要调整

2. **日志系统兼容性**
   - 保持 NativeLogManager 单例不变
   - 确保所有模块的日志记录不受影响

3. **性能考虑**
   - LogFragment 使用 RecyclerView 优化列表性能
   - 日志数量限制避免内存问题
   - 使用 ViewHolder 模式优化列表渲染

4. **用户体验**
   - 页面切换动画流畅
   - 日志实时更新不卡顿
   - 日志颜色区分清晰
