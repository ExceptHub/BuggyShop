# BuggyShop - Automated Error Generation (Cron Jobs)

## 🎯 Overview

BuggyShop now includes **11 automated cron jobs** that continuously generate realistic errors for testing ExceptHub's AI analysis capabilities.

These scheduled tasks run in the background and trigger various types of bugs every 5-30 minutes, providing a continuous stream of realistic error data to ExceptHub.

---

## 📋 Active Cron Jobs

### 1. Product Not Found Error
- **Schedule**: Every 5 minutes
- **What it does**: Attempts to fetch non-existent products (IDs 9999+)
- **Error type**: `NoSuchElementException` / 404 Not Found
- **Test scenario**: User clicks on outdated product link

### 2. Insufficient Inventory Error
- **Schedule**: Every 7 minutes
- **What it does**: Tries to reserve 1000 units of Limited Edition item (only 5 available)
- **Error type**: `InsufficientInventoryException`
- **Test scenario**: Overselling during Black Friday sale

### 3. Expired Coupon Error
- **Schedule**: Every 10 minutes
- **What it does**: Attempts to create order with expired coupon "EXPIRED"
- **Error type**: `BusinessLogicException` - Coupon expired
- **Test scenario**: Customer uses old promotional code

### 4. Invalid Price Update (Validation Error)
- **Schedule**: Every 12 minutes
- **What it does**: Tries to update product with negative price (-99.99)
- **Error type**: `ConstraintViolationException` / Validation error
- **Test scenario**: Admin input error or malicious request

### 5. Concurrent Update Conflict (Optimistic Locking)
- **Schedule**: Every 15 minutes
- **What it does**: Simulates concurrent inventory updates
- **Error type**: `OptimisticLockException`
- **Test scenario**: Two users buying last item simultaneously

### 6. Deleted Product Update Error
- **Schedule**: Every 8 minutes
- **What it does**: Attempts to update a non-existent product (ID 8888)
- **Error type**: `NoSuchElementException`
- **Test scenario**: Updating product that was already deleted

### 7. Invalid State Transition
- **Schedule**: Every 20 minutes
- **What it does**: Creates order, cancels it, then tries to cancel again
- **Error type**: `IllegalStateException` - Cannot cancel cancelled order
- **Test scenario**: Double-click on "Cancel Order" button

### 8. Pagination Out of Bounds
- **Schedule**: Every 6 minutes
- **What it does**: Requests page 9999 of products
- **Error type**: Pagination error / Empty page
- **Test scenario**: Manipulated pagination URL

### 9. Circular Bundle Dependency
- **Schedule**: Every 25 minutes
- **What it does**: Tries to add product to its own bundle
- **Error type**: `BusinessLogicException` - Circular dependency
- **Test scenario**: Admin configuration mistake

### 10. Max Uses Exceeded for Coupon
- **Schedule**: Every 18 minutes
- **What it does**: Attempts to use coupon "LIMITED" that has 5/5 uses
- **Error type**: `BusinessLogicException` - Coupon limit reached
- **Test scenario**: Trying to reuse one-time coupon

### 11. N+1 Query Problem Detector
- **Schedule**: Every 30 minutes
- **What it does**: Fetches all products and accesses lazy-loaded categories in loop
- **Error type**: Performance issue - N+1 queries
- **Test scenario**: Inefficient ORM usage causing slow page loads

---

## ⚙️ Configuration

### Enable/Disable Cron Jobs

**In `application.yml`:**
```yaml
buggyshop:
  scheduler:
    enabled: true  # Set to false to disable all cron jobs
```

**Via Environment Variable:**
```bash
SCHEDULER_ENABLED=false mvn spring-boot:run
```

**Default**: Cron jobs are **ENABLED** by default

---

## 📊 Expected Results in ExceptHub

After running BuggyShop for 30+ minutes, you should see:

### Error Dashboard:
- ✅ **11+ different error types** with unique fingerprints
- ✅ **Multiple occurrences** of each error (pattern detection)
- ✅ **Full stack traces** with exact line numbers
- ✅ **HTTP context** for each error (when applicable)

### AI Analysis for Each Error:
- ✅ **Root cause** explanation
- ✅ **Code location** (file:line)
- ✅ **Suggested fix** with code examples
- ✅ **Impact assessment** (user-facing, data integrity, performance)

### Cron Execution Tracking:
- ✅ **Execution history** for each scheduled job
- ✅ **Success/failure rates**
- ✅ **Execution duration** trends
- ✅ **Error patterns** over time

---

## 🔍 Monitoring Cron Execution

### Check Application Logs

**Look for cron execution markers:**
```
🔴 CRON: Attempting to fetch non-existent product ID: 9999
✅ Expected error generated: No value present
```

**Verify ExceptHub integration:**
```
✅ Cron execution sent to ExceptHub: generateProductNotFoundException
```

### Check Running Status

**See hourly summary:**
```
📊 ERROR GENERATOR STATUS:
  ✅ 11 error-generating crons are active
  📍 Errors are being sent to ExceptHub for AI analysis
  🎯 Testing realistic e-commerce bugs
  ⏰ Next errors will be generated within 5-30 minutes
```

This summary is logged **every hour** (3600000ms).

---

## 🎯 Testing Strategy

### Quick Test (30 minutes):
1. Start BuggyShop with crons enabled (default)
2. Wait 30 minutes for all cron jobs to execute at least once
3. Check ExceptHub dashboard - expect 11+ error types

### Full Test (2 hours):
1. Let BuggyShop run for 2+ hours
2. Observe error patterns and frequency
3. Verify AI analysis quality for each error type
4. Check deduplication (same error should be grouped)

### Manual Testing + Automated:
1. Keep crons enabled (background noise)
2. Run manual tests from `QUICK_TEST.md`
3. See how ExceptHub handles both automated and manual errors
4. Total: 21+ manual + 11 automated = **32+ error types**

---

## 🛠️ Customizing Cron Schedules

To change execution frequency, edit `ErrorGeneratorScheduler.java`:

```java
// Current: Every 5 minutes (300000ms)
@Scheduled(fixedDelay = 300000)
public void generateProductNotFoundException() {
    // ...
}

// Change to: Every 30 seconds (30000ms)
@Scheduled(fixedDelay = 30000)
public void generateProductNotFoundException() {
    // ...
}
```

**Recommended intervals:**
- **Development/Demo**: 30-60 seconds (fast feedback)
- **Testing**: 5-15 minutes (moderate load)
- **Production Simulation**: 15-30 minutes (realistic frequency)

---

## 🚦 Cron Job Lifecycle

```
Application Starts
       ↓
@EnableScheduling activated
       ↓
ErrorGeneratorScheduler bean created
       ↓
11 scheduled methods registered
       ↓
Each method runs on its own schedule
       ↓
Error generated → Caught by @RestControllerAdvice
       ↓
ExceptHub Starter captures error
       ↓
Error sent to ExceptHub API
       ↓
AI analysis performed
       ↓
Result visible in dashboard
```

---

## 📈 Performance Impact

### Resource Usage:
- **CPU**: ~1-2% increase (11 cron jobs)
- **Memory**: ~50MB additional (scheduled task threads)
- **Database**: 1-5 queries per cron execution
- **Network**: 1 HTTP request to ExceptHub per error

### Scalability:
- Safe for local development ✅
- Safe for testing environments ✅
- **DO NOT USE** in production ❌ (intentionally generates errors)

---

## 🔧 Troubleshooting

### Crons Not Running?

**1. Check if scheduling is enabled:**
```bash
# Look for this in logs:
"@EnableScheduling activated"
```

**2. Verify scheduler configuration:**
```yaml
buggyshop:
  scheduler:
    enabled: true  # Must be true
```

**3. Check ConditionalOnProperty:**
```bash
# Look for this in logs:
"ErrorGeneratorScheduler bean created"
```

### Not Seeing Errors in ExceptHub?

**1. Check ExceptHub configuration:**
```yaml
excepthub:
  enabled: true
  api-key: eak_your_key_here
  endpoint: https://api.excepthub.dev/api/v1/errors
```

**2. Verify ExceptHub logs:**
```bash
# Look for:
"Cron execution sent to ExceptHub"
"Error sent successfully"
```

**3. Check network connectivity:**
```bash
curl -X POST https://api.excepthub.dev/api/v1/errors \
  -H "X-API-Key: eak_your_key" \
  -H "Content-Type: application/json" \
  -d '{"test":"connection"}'
```

---

## 🎓 Learning Outcomes

### For Developers:
- Understanding how @Scheduled works in Spring
- Learning about different error patterns
- Seeing how exceptions propagate through layers
- Observing AI error analysis in action

### For QA/Testers:
- Realistic error scenarios for testing
- Continuous error generation for load testing
- Automated regression testing for error handling
- Pattern analysis for error frequency

### For Product Teams:
- Understanding common e-commerce bugs
- Seeing impact of errors on user experience
- Learning from AI-suggested fixes
- Planning error prevention strategies

---

## 📚 Related Documentation

- **Main Guide**: `README.md` - Project overview
- **Manual Testing**: `TESTING_GUIDE.md` - 21 manual test scenarios
- **Quick Tests**: `QUICK_TEST.md` - Top 10 critical errors
- **Project Summary**: `PROJECT_SUMMARY.md` - Architecture & features

---

## ✨ Key Features

### Realistic Scenarios:
- ❌ NOT: `throw new RuntimeException("test")`
- ✅ YES: Actual business logic failures

### Diverse Error Categories:
- Database errors (not found, constraints, deadlocks)
- Business logic errors (inventory, coupons, states)
- Validation errors (negative prices, invalid input)
- Concurrency errors (race conditions, optimistic locking)
- Performance issues (N+1 queries, slow operations)

### Production-Like Behavior:
- Errors happen randomly (via Random IDs)
- Multiple users simulated (userId 1 & 2)
- Different error frequencies (5-30 min intervals)
- Real exception types (not generic RuntimeException)

---

## 🎯 Success Metrics

After 1 hour of running:
- ✅ All 11 cron jobs should have executed at least once
- ✅ ExceptHub should show 11+ unique error fingerprints
- ✅ Each error should have AI analysis
- ✅ No application crashes or hangs

After 2 hours:
- ✅ Pattern detection should identify recurring errors
- ✅ Error frequency charts should show regular intervals
- ✅ Cron execution tracking should show success rates
- ✅ Performance impact should remain minimal (<5% CPU)

---

**Happy Error Testing!** 🐛🔍🚀

For questions or issues, check logs for detailed error information.
