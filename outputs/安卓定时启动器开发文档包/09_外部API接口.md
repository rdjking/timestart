# 外部 API 接口文档（可选：节假日与调休）

## 1. 边界

应用无自建后端，本接口只在用户开启“中国法定节假日与调休”时使用。基础“周一至周五/周六日”规则必须完全离线可用。

建议使用一个可替换的 `HolidayDataSource`，不要把供应商 URL 散落在业务代码中。供应商接入前应复核可用性、授权条款、限流和隐私政策。

## 2. 领域接口

```kotlin
interface HolidayDataSource {
    suspend fun getDay(date: LocalDate): HolidayDay?
    suspend fun refreshYear(year: Int): RefreshResult
}

data class HolidayDay(
    val date: LocalDate,
    val dayType: HolidayDayType, // WORKDAY, REST_DAY, UNKNOWN
    val label: String?,
    val source: String,
    val expiresAt: Instant
)
```

业务层只依赖以上接口，不依赖某一第三方 JSON 结构。

## 3. 推荐请求契约

以下为适配器对外的逻辑契约，不绑定具体服务商：

```http
GET /holiday/info/{yyyy-MM-dd}
Accept: application/json
```

### 正常响应（适配后的内部格式）

```json
{
  "date": "2026-10-10",
  "dayType": "WORKDAY",
  "label": "国庆节调休工作日",
  "source": "provider-name",
  "fetchedAt": "2026-07-15T10:00:00Z",
  "expiresAt": "2026-12-31T15:59:59Z"
}
```

### 错误映射

| HTTP/异常 | `RefreshResult` | 后续行为 |
|---|---|---|
| 200 + 合法 JSON | `Success` | 写入缓存。 |
| 404/日期无数据 | `NoData` | 使用普通星期规则。 |
| 429/5xx/网络超时 | `Unavailable` | 使用未过期缓存；否则普通星期规则。 |
| JSON 字段不完整 | `Malformed` | 不写入缓存，记录系统日志。 |

## 4. 缓存和降级

- 按日期缓存；有效期至当年最后一天，次年首次使用时刷新。
- 仅在 App 前台、用户点按“刷新”或非精确的维护任务中联网；闹钟 Receiver 不联网。
- 离线、缓存过期或服务失败时，`WEEKDAY` = 周一至周五，`WEEKEND` = 周六日；UI 显示“已按普通星期规则计算”。
- 无论接口返回什么，都不可覆盖用户的单次日期、每月日期或年度日期规则。

## 5. 安全与隐私

- 请求不发送设备标识、任务名称、目标包名或日志。
- 仅传日期参数；使用 HTTPS 和 OkHttp 默认 TLS 校验。
- 不把第三方响应原文展示给用户或当作指令执行。
