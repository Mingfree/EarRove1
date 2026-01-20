# <font style="color:rgb(31, 31, 31);">产品需求文档 (PRD): EarRove 聆途 - 视障辅助平台</font>
| **<font style="color:rgb(31, 31, 31);">文档版本</font>** | **<font style="color:rgb(31, 31, 31);">日期</font>** | **<font style="color:rgb(31, 31, 31);">状态</font>** | **<font style="color:rgb(31, 31, 31);">备注</font>** |
| --- | --- | --- | --- |
| <font style="color:rgb(31, 31, 31);">v1.0</font> | <font style="color:rgb(31, 31, 31);">2026-01-17</font> | **<font style="color:rgb(31, 31, 31);">草稿版</font>** | <font style="color:rgb(31, 31, 31);">确立本地化AI策略，移除盲道视觉检测，固化震动交互规范</font> |


## <font style="color:rgb(31, 31, 31);">1. 项目概述</font>
<font style="color:rgb(31, 31, 31);">本项目旨在开发一款运行于 Android 平台的视障辅助应用，通过整合</font>**<font style="color:rgb(31, 31, 31);">百度地图SDK</font>**<font style="color:rgb(31, 31, 31);">（导航）、</font>**<font style="color:rgb(31, 31, 31);">Google ML Kit</font>**<font style="color:rgb(31, 31, 31);">（本地文字识别）与</font>**<font style="color:rgb(31, 31, 31);">多模态交互</font>**<font style="color:rgb(31, 31, 31);">（语音+震动），为视障人士提供低延迟、高隐私保护的出行与生活辅助。</font>

## <font style="color:rgb(31, 31, 31);">2. 用户角色</font>
+ **<font style="color:rgb(31, 31, 31);">终端用户</font>**<font style="color:rgb(31, 31, 31);">：视力障碍人士（全盲或低视力）。</font>
+ **<font style="color:rgb(31, 31, 31);">管理员</font>**<font style="color:rgb(31, 31, 31);">：负责后端服务维护及用户反馈处理。</font>

## <font style="color:rgb(31, 31, 31);">3. 功能需求 (Functional Requirements)</font>
### <font style="color:rgb(31, 31, 31);">3.1 核心模块：多模态导航 (Navigation)</font>
+ **<font style="color:rgb(31, 31, 31);">F-NAV-01 语音指令交互</font>**
    - <font style="color:rgb(31, 31, 31);">用户通过语音输入目的地（如“去中央民族大学”）。</font>
    - <font style="color:rgb(31, 31, 31);">系统需在 0.5 秒内响应并确认目的地。</font>
+ **<font style="color:rgb(31, 31, 31);">F-NAV-02 基础步行导航</font>**
    - <font style="color:rgb(31, 31, 31);">集成百度地图步行导航 SDK，提供转弯、直行等语音指引。</font>
    - _<font style="color:rgb(31, 31, 31);">变更说明：移除“导向盲道”的精细指令，仅保留标准路径引导。</font>_
+ **<font style="color:rgb(31, 31, 31);">F-NAV-03 实时路况与红绿灯</font>**
    - <font style="color:rgb(31, 31, 31);">调用百度 Lamp API，在接近路口时自动播报红绿灯倒计时（如“红灯还有 15 秒”）。</font>
+ **<font style="color:rgb(31, 31, 31);">F-NAV-04 主动避障 (Obstacle Detection)</font>**
    - <font style="color:rgb(31, 31, 31);">利用手机摄像头实时检测前方通用障碍物（车辆、柱子、墙体）。</font>
    - **<font style="color:rgb(31, 31, 31);">冲突仲裁机制</font>**<font style="color:rgb(31, 31, 31);">：当“导航语音”与“避障语音”同时触发时，系统</font>**<font style="color:rgb(31, 31, 31);">强制中断</font>**<font style="color:rgb(31, 31, 31);">导航播报，优先播放避障警示，待危险解除后恢复导航。</font>
+ **<font style="color:rgb(31, 31, 31);">F-NAV-05 震动编码反馈 (Haptic Feedback)</font>**
    - **<font style="color:rgb(31, 31, 31);">障碍物警示</font>**<font style="color:rgb(31, 31, 31);">：短震 2 次 (</font>`<font style="color:rgb(68, 71, 70);">Short-Short</font>`<font style="color:rgb(31, 31, 31);">) —— 频率：急促。</font>
    - **<font style="color:rgb(31, 31, 31);">转向提示</font>**<font style="color:rgb(31, 31, 31);">：长震 1 次 (</font>`<font style="color:rgb(68, 71, 70);">Long</font>`<font style="color:rgb(31, 31, 31);">) —— 频率：平缓，配合语音“请左转/右转”。</font>

### <font style="color:rgb(31, 31, 31);">3.2 核心模块：视觉辅助 (Visual Assistance)</font>
+ **<font style="color:rgb(31, 31, 31);">F-VIS-01 本地文字识别 (OCR)</font>**
    - <font style="color:rgb(31, 31, 31);">集成 </font>**<font style="color:rgb(31, 31, 31);">Google ML Kit (Bundled版)</font>**<font style="color:rgb(31, 31, 31);">，模型内置于 App 中，无需联网。</font>
    - **<font style="color:rgb(31, 31, 31);">实时模式</font>**<font style="color:rgb(31, 31, 31);">：摄像头对准文字区域（如路牌、药品名），实时朗读识别到的文本。</font>
    - **<font style="color:rgb(31, 31, 31);">隐私保护</font>**<font style="color:rgb(31, 31, 31);">：图像数据仅在内存中处理，不上传服务器，不保存到本地相册。</font>
+ **<font style="color:rgb(31, 31, 31);">F-VIS-02 暗光增强</font>**
    - <font style="color:rgb(31, 31, 31);">检测到环境光线不足时，自动开启手机闪光灯补光。</font>

### <font style="color:rgb(31, 31, 31);">3.3 通用模块：无障碍体验 (Accessibility)</font>
+ **<font style="color:rgb(31, 31, 31);">F-UI-01 读屏适配</font>**<font style="color:rgb(31, 31, 31);">：全线控件支持 TalkBack，添加 </font>`<font style="color:rgb(68, 71, 70);">contentDescription</font>`<font style="color:rgb(31, 31, 31);">。</font>
+ **<font style="color:rgb(31, 31, 31);">F-UI-02 高对比度 UI</font>**<font style="color:rgb(31, 31, 31);">：黑底黄字/白字大号字体设计。</font>

---

## <font style="color:rgb(31, 31, 31);">4. 系统架构图 (System Architecture)</font>
<font style="color:rgb(31, 31, 31);">由于采用了本地 OCR 策略，后端架构大幅简化。系统呈现 </font>**<font style="color:rgb(31, 31, 31);">"重前端 (Thick Client) - 轻后端 (Thin Server)"</font>**<font style="color:rgb(31, 31, 31);"> 的形态。</font>

```mermaid
graph TD
    %% 客户端层
    subgraph "Android Client (用户端)"
        UI[无障碍 UI 层]
        
        subgraph "核心业务逻辑"
            Manager[任务调度管理器]
            Arbitrator[音频/震动仲裁器]
        end
        
        subgraph "本地 AI & SDK 能力"
            MLKit[Google ML Kit (本地 OCR)]
            BaiduSDK[百度地图 SDK (导航)]
            Camera[CameraX 图像流]
        end
    end

    %% 服务端层
    subgraph "Python Server (轻量后端)"
        API_Gateway[API 网关]
        UserDB[(用户数据库)]
        Proxy[百度 API 代理/鉴权]
    end

    %% 外部服务
    subgraph "Third Party (外部云)"
        BaiduCloud[百度地图云服务]
    end

    %% 连线关系
    UI --> Manager
    Manager --> MLKit
    Manager --> BaiduSDK
    Camera --> MLKit
    
    %% 关键逻辑：仲裁器控制输出
    BaiduSDK -- 导航音频流 --> Arbitrator
    MLKit -- 识别文本流 --> Arbitrator
    Manager -- 避障信号 --> Arbitrator
    Arbitrator --> Speaker[手机扬声器]
    Arbitrator --> Motor[震动马达]

    %% 网络请求
    BaiduSDK <-->|地图数据/GPS| BaiduCloud
    Manager <-->|登录/同步/红绿灯请求| API_Gateway
    API_Gateway <--> UserDB
    Proxy <-->|Token鉴权| BaiduCloud
```

### <font style="color:rgb(31, 31, 31);">架构说明：</font>
1. **<font style="color:rgb(31, 31, 31);">Android Client (核心)</font>**<font style="color:rgb(31, 31, 31);">：</font>
    - **<font style="color:rgb(31, 31, 31);">任务调度管理器</font>**<font style="color:rgb(31, 31, 31);">：负责在“导航模式”和“文字识别模式”之间切换，避免资源冲突。</font>
    - **<font style="color:rgb(31, 31, 31);">仲裁器 (Arbitrator)</font>**<font style="color:rgb(31, 31, 31);">：这是你项目中非常关键的组件。它接收来自百度SDK的导航音、OCR的朗读音和避障模块的警报，根据优先级（避障 > 导航 > OCR）决定谁能占用扬声器和震动马达。</font>
    - **<font style="color:rgb(31, 31, 31);">ML Kit</font>**<font style="color:rgb(31, 31, 31);">：完全内嵌，不产生网络流量。</font>
2. **<font style="color:rgb(31, 31, 31);">Python Server (辅助)</font>**<font style="color:rgb(31, 31, 31);">：</font>
    - <font style="color:rgb(31, 31, 31);">主要用于用户账户管理（如保存用户的常用地点）。</font>
    - <font style="color:rgb(31, 31, 31);">作为</font>**<font style="color:rgb(31, 31, 31);">红绿灯 API (Lamp API)</font>**<font style="color:rgb(31, 31, 31);"> 的中转站（如果百度API需要服务器端鉴权，或者为了隐藏你的 API Key，建议通过自己的后端转发请求）。</font>

---

## <font style="color:rgb(31, 31, 31);">5. 数据流转图 (Data Flow Diagram)</font>
<font style="color:rgb(31, 31, 31);">展示数据如何在各模块间流动，以及冲突是如何被处理的。</font>

### <font style="color:rgb(31, 31, 31);">场景：用户正在导航中，前方突然出现障碍物，且正经过红绿灯路口。</font>
<!-- 这是一个文本绘图，源码为：sequenceDiagram
    participant User as 用户
    participant Sensor as 传感器(相机/GPS)
    participant NavEngine as 百度导航SDK
    participant Vision as 避障/OCR模块
    participant Arbitrator as 冲突仲裁器
    participant Output as 扬声器/马达

    %% 1. 正常导航流
    Sensor->>NavEngine: 实时 GPS 坐标
    NavEngine->>Arbitrator: 语音："前方100米直行"
    Arbitrator->>Output: 播放语音 (优先级:中)

    %% 2. 红绿灯数据流
    NavEngine->>Sensor: 请求路况接口
    Sensor-->>NavEngine: 返回：红灯剩余 20秒
    NavEngine->>Arbitrator: 语音："前方路口红灯20秒"
    Arbitrator->>Output: 播放语音 (优先级:中)

    %% 3. 突发障碍物 (冲突场景)
    loop 每一帧图像检测
        Sensor->>Vision: 摄像头预览流 (YUV/Bitmap)
        Vision->>Vision: 图像分析 (是否有障碍?)
        
        alt 检测到障碍物
            Vision->>Arbitrator: 信号：警报 (距离<1米)
            
            Note over Arbitrator: 检测到高优先级信号！
            Note over Arbitrator: 立即暂停导航语音
            
            Arbitrator->>Output: 触发震动 (短震两下)
            Arbitrator->>Output: 语音："前方有障碍" (优先级:高)
        end
    end

    %% 4. 恢复
    Note over Vision: 障碍物消失
    Arbitrator->>Output: 恢复导航语音队列 -->
![](https://cdn.nlark.com/yuque/__mermaid_v3/74c761c25c5f798317bebcfae2aceaad.svg)

### <font style="color:rgb(31, 31, 31);">数据流说明：</font>
1. **<font style="color:rgb(31, 31, 31);">视频流 (Video Stream)</font>**<font style="color:rgb(31, 31, 31);">：从 CameraX 采集数据，直接送入本地算法处理，</font>**<font style="color:rgb(31, 31, 31);">不经过网络，不存盘</font>**<font style="color:rgb(31, 31, 31);">。处理结果转化为“控制信号”发给仲裁器。</font>
2. **<font style="color:rgb(31, 31, 31);">音频流 (Audio Stream)</font>**<font style="color:rgb(31, 31, 31);">：导航语音和 TTS（文字转语音）生成音频流，由仲裁器进行“混音”或“抢占”。</font>
3. **<font style="color:rgb(31, 31, 31);">网络流 (Network Data)</font>**<font style="color:rgb(31, 31, 31);">：仅限 GPS 坐标上传和地图/红绿灯数据下行，带宽占用极低。</font>



