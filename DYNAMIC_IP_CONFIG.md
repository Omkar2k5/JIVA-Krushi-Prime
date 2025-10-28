# Dynamic Server IP Configuration

## Overview
This feature allows the JIVA app to dynamically update its server IP address without requiring an app update or restart. Users can click an "Update" button on the home screen to fetch the latest server configuration from the API.

## Implementation Summary

### 1. API Models (`AppInfoModels.kt`)
- Created `AppInfoResponse` and `AppInfoData` data classes
- Handles response from `GET /api/JivaBusiness/GetAppInfo`
- Response format:
```json
{
  "isSuccess": true,
  "message": "Success",
  "data": {
    "appName": "Jiva Business",
    "appVersion": "1.0.0",
    "ip": "103.48.42.125:8081"
  }
}
```

### 2. Storage (`UserEnv.kt`)
- Added `setServerIp()`, `getServerIp()`, and `clearServerIp()` methods
- Server IP stored in SharedPreferences under key `server_ip`
- Persists across app restarts

### 3. Dynamic RetrofitClient (`RetrofitClient.kt`)
- **Refactored from static singleton to dynamic configuration**
- Added `initialize(Context)` - loads stored IP on app startup
- Added `updateBaseUrl(String)` - updates Retrofit instance with new IP at runtime
- Added `formatBaseUrl(String)` - normalizes IP format (adds http:// and trailing /)
- Default fallback: `http://103.48.42.125:8081/`
- Thread-safe with `@Volatile` and `@Synchronized`

### 4. API Endpoint (`JivaApiService.kt`)
- Added `getAppInfo()` endpoint: `GET /api/JivaBusiness/GetAppInfo`

### 5. ViewModel (`HomeViewModel.kt`)
- Added `updateServerConfig()` method
- Fetches server config from API
- Updates storage and RetrofitClient
- Shows success/error messages via UI state
- Added `isUpdatingConfig` and `updateSuccess` to `HomeUiState`

### 6. UI (`HomeScreen.kt`)
- Added "Update" button (refresh icon) next to Logout button in header
- Same size and style as Logout button
- Shows loading spinner while updating
- Displays toast notification on success/failure

### 7. App Initialization (`JivaApplication.kt`)
- Added `initializeRetrofitClient()` called early in `onCreate()`
- Loads stored IP before any API calls
- Falls back to default IP if none stored

## Flow

### Initial App Launch
1. App starts → `JivaApplication.onCreate()`
2. `initializeRetrofitClient()` called
3. Checks `UserEnv.getServerIp()`
4. If IP stored → uses it; else → uses default
5. All API calls use configured IP

### User Updates Server Config
1. User taps Update button on HomeScreen
2. `HomeViewModel.updateServerConfig()` called
3. API call to current server: `GET /api/JivaBusiness/GetAppInfo`
4. Server returns new IP (e.g., `103.48.42.125:8081`)
5. IP saved to `UserEnv.setServerIp()`
6. `RetrofitClient.updateBaseUrl()` recreates Retrofit with new IP
7. All subsequent API calls use new IP
8. Success toast shown to user

## Potential Loopholes & Considerations

### ⚠️ Critical Issues

1. **Chicken-and-Egg Problem**
   - **Issue**: To get the new IP, we need to call the API using the old/current IP
   - **Current behavior**: Update button fetches from current server
   - **Problem**: If current server is down, can't get new IP
   - **Mitigation**: User must update when old server is still accessible
   - **Alternative solution**: Hardcode a "discovery" URL or use DNS

2. **No Validation**
   - **Issue**: IP format not validated before storage
   - **Risk**: Malformed IP breaks all API calls
   - **Current mitigation**: `formatBaseUrl()` adds http:// and /
   - **Recommendation**: Add IP/URL validation regex

3. **No HTTPS**
   - **Issue**: Using HTTP, not HTTPS
   - **Risk**: Man-in-the-middle attacks, data interception
   - **Current state**: `network_security_config.xml` allows cleartext
   - **Recommendation**: Move to HTTPS in production

4. **Race Conditions**
   - **Issue**: Multiple simultaneous API calls during IP update
   - **Current mitigation**: `@Synchronized` on `updateBaseUrl()`
   - **Remaining risk**: In-flight requests may fail during transition
   - **Recommendation**: Add request queue retry logic

### ⚠️ Secondary Issues

5. **No Fallback List**
   - **Issue**: Only one IP stored; if it fails, user is stuck
   - **Recommendation**: Store list of fallback IPs

6. **No Version Check**
   - **Issue**: API returns `appVersion` but we don't validate
   - **Risk**: Old app version may be incompatible with new server
   - **Recommendation**: Add version compatibility check

7. **User Can Break App**
   - **Issue**: Clicking Update repeatedly or when server down
   - **Current mitigation**: Button disabled during update
   - **Recommendation**: Add cooldown period (e.g., 10 seconds)

8. **No Undo**
   - **Issue**: If new IP is wrong, user can't revert
   - **Recommendation**: Store previous IP, add "Revert" option

9. **Storage Not Encrypted**
   - **Issue**: IP stored in plain SharedPreferences
   - **Risk**: Minor (IP is not secret), but best practice is encryption
   - **Recommendation**: Use EncryptedSharedPreferences

10. **No Connection Test**
    - **Issue**: New IP saved without testing connectivity
    - **Recommendation**: Ping new IP before committing

11. **UI Confusion**
    - **Issue**: Update button icon (Refresh) may confuse users
    - Users might think it refreshes data, not updates server
    - **Recommendation**: Use Settings icon or add tooltip

12. **No Admin Protection**
    - **Issue**: Any user can change server IP
    - **Risk**: Non-admin users could misconfigure
    - **Recommendation**: Add admin-only check

### 🔧 Suggested Improvements

```kotlin
// 1. Add IP validation
fun isValidServerIp(ip: String): Boolean {
    val regex = Regex("^https?://[\\w\\d.:-]+/?$")
    return regex.matches(ip)
}

// 2. Store IP history
fun setServerIpWithHistory(context: Context, newIp: String) {
    val currentIp = getServerIp(context)
    if (currentIp != null) {
        setString(context, "previous_server_ip", currentIp)
    }
    setServerIp(context, newIp)
}

// 3. Test connectivity before saving
suspend fun testServerConnectivity(ip: String): Boolean {
    return try {
        val testRetrofit = createRetrofit(formatBaseUrl(ip))
        val testService = testRetrofit.create(JivaApiService::class.java)
        testService.getAppInfo()
        true
    } catch (e: Exception) {
        false
    }
}

// 4. Add version compatibility check
if (response.data.appVersion > BuildConfig.VERSION_NAME) {
    // Show "Update app from Play Store" dialog
}
```

## Testing Checklist

- [ ] Fresh install → uses default IP
- [ ] Click Update → fetches and stores new IP
- [ ] Restart app → uses stored IP
- [ ] Update with malformed IP → handles error gracefully
- [ ] Update while offline → shows error message
- [ ] Update while another API call in progress → no crash
- [ ] Logout and login → IP persists
- [ ] Clear app data → reverts to default IP

## Security Notes

- Server must validate API calls to prevent IP spoofing
- Consider adding authentication to GetAppInfo endpoint
- Log all IP changes for audit trail
- Monitor for unusual IP change patterns (abuse detection)

## Migration Path to HTTPS

1. Update server to support HTTPS
2. Get SSL certificate
3. Update `formatBaseUrl()` to use https://
4. Update `network_security_config.xml` to disallow cleartext
5. Test with all API endpoints
6. Deploy server and app together

## Conclusion

The implementation successfully allows dynamic IP configuration, but several production considerations remain:

**Must-fix for production:**
- Add HTTPS support
- Implement IP validation
- Add connectivity testing before saving

**Nice-to-have:**
- IP history/undo
- Fallback IP list
- Admin-only access
- Rate limiting on updates

The core functionality works as specified, but production deployment requires additional hardening.
