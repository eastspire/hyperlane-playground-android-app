# Implementation Plan

- [x] 1. 创建 LogFragment 和布局文件
  - 创建 LogFragment.java 类，实现 NativeLogManager.LogListener 接口
  - 创建 fragment_log.xml 布局文件，包含标题栏、日志级别选择器、日志列表和清除按钮
  - 初始化 UI 组件（RecyclerView、RadioGroup、Button）
  - _Requirements: 1.1, 1.2, 3.1, 3.2, 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 2. 实现 LogFragment 核心功能
  - 实现日志级别切换逻辑（ERROR/INFO/DEBUG）
  - 实现清除日志功能
  - 注册和注销日志监听器
  - 实现 onLogAdded 回调，实时更新日志列表
  - 加载初始日志数据
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4_

- [x] 3. 修改 ViewPagerAdapter 添加 LogFragment
  - 修改 getItemCount() 返回 5（原来是 4）
  - 在 createFragment() 中添加 case 0 返回 LogFragment
  - 调整其他页面的索引：ChatFragment 改为 case 1，ExploreFragment 改为 case 2，TraceFragment 改为 case 3，ProfileFragment 改为 case 4
  - _Requirements: 1.3, 1.4, 2.1, 2.2, 2.3, 2.4_

- [x] 4. 清理 ChatFragment 中的日志面板代码
  - 移除日志面板相关的成员变量（logPanel, btnToggleLog, logRecyclerView, logAdapter, logLevelGroup, btnClearLogs）
  - 移除 NativeLogManager.LogListener 接口实现
  - 移除 setupLogPanel()、toggleLogPanel()、loadLogs()、onLogAdded() 方法
  - 移除 initViews() 中的日志面板初始化代码
  - 保留日志记录功能（NativeLogManager.getInstance().e/i/d 调用）
  - _Requirements: 1.1_

- [x] 5. 更新 fragment_chat.xml 布局文件
  - 移除 btn_toggle_log ImageButton
  - 移除整个 log_panel LinearLayout（包括日志级别选择器、日志列表、清除按钮）
  - 恢复 chat_recycler_view 的约束：移除 app:layout_constraintStart_toEndOf="@id/log_panel"，改为 app:layout_constraintStart_toStartOf="parent"
  - 恢复 chat_recycler_view 的宽度为 match_parent（如果之前改为 0dp）
  - _Requirements: 1.1_

- [x] 6. 调整 MainActivity 默认页面索引
  - 检查 MainActivityWithViewPager.java 中的 setCurrentItem() 调用
  - 如果当前默认显示 ExploreFragment，将索引从 1 改为 2
  - 如果需要默认显示 ChatFragment，将索引设置为 1
  - 如果需要默认显示 LogFragment，将索引设置为 0
  - _Requirements: 2.4_

- [x] 7. 验证日志系统集成
  - 确认 WebSocketManager 中的日志记录正常工作
  - 确认 ChatFragment 中的日志记录正常工作
  - 确认 ChatApplication 中的日志记录正常工作
  - 在 LogFragment 中验证能看到所有模块的日志
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8. 测试页面切换和日志显示
  - 测试从 LogFragment 左滑到 ChatFragment
  - 测试从 ChatFragment 右滑到 LogFragment
  - 测试日志级别切换功能
  - 测试清除日志功能
  - 测试日志实时更新
  - 测试日志时间逆序排序
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 4.1, 4.2, 5.1, 6.1, 6.2, 6.3, 6.4, 6.5_
