# 📘 Complete Testing Guide for Business Domain Components

**Service Layer Tests:**

- ✅ CustomerService - Customer CRUD operations
- ✅ VehicleService - Vehicle management
- ✅ AppointmentService - Appointment lifecycle
- ✅ ProjectService - Project management, status updates
- ✅ TaskService - Task CRUD, status changes
- ✅ TimeLogService - Time tracking, calculations
- ✅ EmployeeService - Employee-specific operations

**Controller Tests:**

- ✅ CustomerController - Customer endpoints
- ✅ VehicleController - Vehicle endpoints
- ✅ AppointmentController - Appointment endpoints
- ✅ ProjectController - Project endpoints
- ✅ TaskController - Task endpoints
- ✅ TimeLogController - Time log endpoints
- ✅ EmployeeController - Employee profile endpoints

**GraphQL Tests:**

- ✅ SearchGraphQLController - GraphQL search queries
- ✅ GraphQLHealthCheckController - GraphQL health checks

---

## 📂 Test Structure

```
src/test/java/com/ead/gearup/
├── unit/
│   └── service/
│       ├── CustomerServiceUnitTest.java        ✅ COMPLETE (33 tests)
│       ├── VehicleServiceUnitTest.java         ✅ COMPLETE (20 tests)
│       ├── AppointmentServiceUnitTest.java     ✅ COMPLETE (21 tests)
│       ├── ProjectServiceUnitTest.java         ✅ COMPLETE (17 tests)
│       ├── TaskServiceUnitTest.java            ✅ COMPLETE (15 tests)
│       ├── TimeLogServiceUnitTest.java         ✅ COMPLETE (12 tests)
│       └── EmployeeServiceUnitTest.java        ✅ COMPLETE (21 tests)
├── integration/
│   ├── controller/
│   │   ├── CustomerControllerIntegrationTest.java        ✅ COMPLETE (17 tests)
│   │   ├── VehicleControllerIntegrationTest.java         ✅ COMPLETE (17 tests)
│   │   ├── AppointmentControllerIntegrationTest.java     ✅ COMPLETE (20 tests)
│   │   ├── ProjectControllerIntegrationTest.java         ✅ COMPLETE (18 tests)
│   │   ├── TaskControllerIntegrationTest.java            ✅ COMPLETE (18 tests)
│   │   ├── TimeLogControllerIntegrationTest.java         ✅ COMPLETE (17 tests)
│   │   └── EmployeeControllerIntegrationTest.java        ✅ COMPLETE (19 tests)
│   └── graphql/
│       ├── SearchGraphQLControllerTest.java              ✅ COMPLETE (14 tests)
│       └── GraphQLHealthCheckControllerTest.java         ✅ COMPLETE (5 tests)
├── fixtures/           # Test data builders
└── helpers/            # Test utility classes
```

---

## 🧪 Understanding Test Types

### 1. **Unit Tests** (Service Layer)

**Purpose:** Test business logic in isolation
**Characteristics:**

- Fast execution
- Mock all dependencies
- Focus on one method at a time
- No Spring context needed

**Example Pattern:**

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {
    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void testGetById_Success() {
        // Arrange - Setup test data and mock behavior
        when(customerRepository.findById(1L))
            .thenReturn(Optional.of(testCustomer));

        // Act - Call the method
        CustomerResponseDTO result = customerService.getById(1L);

        // Assert - Verify the result
        assertNotNull(result);
        assertEquals(1L, result.getCustomerId());
        verify(customerRepository, times(1)).findById(1L);
    }
}
```

### 2. **Integration Tests** (Controller Layer)

**Purpose:** Test the full HTTP request-response cycle
**Characteristics:**

- Uses Spring Boot Test context
- Tests REST endpoints
- Validates request/response mapping
- Tests security and validation

**Example Pattern:**

```java
@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }
}
```

### 3. **GraphQL Tests** (GraphQL Controller Layer)

**Purpose:** Test GraphQL query endpoints
**Characteristics:**

- Uses Spring Boot Test context
- Direct controller method invocation
- Tests GraphQL query logic
- Validates search functionality

**Example Pattern:**

```java
@SpringBootTest
class SearchGraphQLControllerTest {
    @Autowired
    private SearchGraphQLController controller;

    @MockBean
    private CustomerService customerService;

    @Test
    void testSearchCustomers_Success() {
        // Arrange
        List<CustomerSearchResponseDTO> customers =
            Arrays.asList(testCustomer);
        when(customerService.searchCustomersByCustomerName("John"))
            .thenReturn(customers);

        // Act
        List<CustomerSearchResponseDTO> result =
            controller.searchCustomersByCustomerName("John");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(customerService, times(1))
            .searchCustomersByCustomerName("John");
    }
}
```

---

## ▶️ Running Tests

### Run All Tests

```powershell
mvn test
```

### Run Specific Test Class

```powershell
mvn test -Dtest=CustomerServiceUnitTest
```

### Run All Service Unit Tests

```powershell
mvn test -Dtest="*ServiceUnitTest"
```

### Run All Controller Integration Tests

```powershell
mvn test -Dtest="*ControllerIntegrationTest"
```

### Run All GraphQL Tests

```powershell
mvn test -Dtest="*GraphQL*Test"
```

### Run With Coverage Report

```powershell
mvn clean test jacoco:report
```

Then view report at: `target/site/jacoco/index.html`

---

## 📝 Test Coverage Guidelines

### What to Test in Each Service Method:

#### 1. **Success Scenarios** ✅

- Valid input returns expected output
- Correct repository methods are called
- Proper data transformations occur

#### 2. **Failure Scenarios** ❌

- Resource not found (throwNotFoundException)
- Invalid input (throw IllegalArgumentException)
- Unauthorized access (throw UnauthorizedException)
- Business rule violations

#### 3. **Edge Cases** 🔍

- Null values
- Empty lists
- Boundary conditions (0, negative numbers)
- Role-based access control

### Example Coverage for CustomerService.getById():

```java
// ✅ Success case
@Test
void testGetById_Success() { ... }

// ❌ Not found
@Test
void testGetById_NotFound() { ... }

// ❌ Invalid ID - null
@Test
void testGetById_InvalidId_Null() { ... }

// ❌ Invalid ID - zero
@Test
void testGetById_InvalidId_Zero() { ... }

// ❌ Invalid ID - negative
@Test
void testGetById_InvalidId_Negative() { ... }
```

---

## 🎨 Test Patterns and Best Practices

### 1. **AAA Pattern** (Arrange-Act-Assert)

```java
@Test
void testMethodName() {
    // Arrange - Setup test data
    Customer customer = new Customer();
    when(repository.findById(1L)).thenReturn(Optional.of(customer));

    // Act - Execute the method
    CustomerResponseDTO result = service.getById(1L);

    // Assert - Verify results
    assertNotNull(result);
    verify(repository, times(1)).findById(1L);
}
```

### 2. **Given-When-Then** (BDD Style)

```java
@Test
void shouldReturnCustomer_whenValidIdProvided() {
    // Given
    Long customerId = 1L;
    Customer customer = createTestCustomer();
    when(repository.findById(customerId))
        .thenReturn(Optional.of(customer));

    // When
    CustomerResponseDTO result = service.getById(customerId);

    // Then
    assertNotNull(result);
    assertEquals(customerId, result.getCustomerId());
}
```

### 3. **Test Naming Conventions**

```java
// Pattern: test<MethodName>_<Scenario>_<ExpectedResult>
testGetById_Success()
testGetById_NotFound()
testCreate_InvalidInput()
testUpdate_UnauthorizedAccess()

// Or BDD style:
shouldReturnCustomer_whenValidIdProvided()
shouldThrowException_whenCustomerNotFound()
```

---

## 🔧 Common Mockito Patterns

### Mock Repository Methods

```java
// Return value
when(repository.findById(1L)).thenReturn(Optional.of(entity));

// Return empty
when(repository.findById(999L)).thenReturn(Optional.empty());

// Return list
when(repository.findAll()).thenReturn(Arrays.asList(entity1, entity2));

// Return empty list
when(repository.findAll()).thenReturn(Arrays.asList());

// Void methods
doNothing().when(repository).deleteById(1L);

// Throw exception
when(repository.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));
```

### Verify Interactions

```java
// Verify method was called once
verify(repository, times(1)).findById(1L);

// Verify method was never called
verify(repository, never()).save(any());

// Verify with argument matchers
verify(repository).save(argThat(customer ->
    customer.getName().equals("John")));
```

### Argument Matchers

```java
any()                   // Any object
any(Customer.class)     // Any Customer object
anyLong()               // Any long value
anyString()             // Any string
eq(1L)                  // Exactly 1L
```

---

## 🛠️ Next Steps: Controller Integration Tests

### Basic Controller Test Template

```java
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerResponseDTO testCustomerResponse;

    @BeforeEach
    void setUp() {
        testCustomerResponse = new CustomerResponseDTO();
        testCustomerResponse.setCustomerId(1L);
        testCustomerResponse.setName("Test Customer");
        testCustomerResponse.setEmail("test@example.com");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllCustomers_Success() throws Exception {
        // Arrange
        List<CustomerResponseDTO> customers = Arrays.asList(testCustomerResponse);
        when(customerService.getAll()).thenReturn(customers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].customerId").value(1));

        verify(customerService, times(1)).getAll();
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testCreateCustomer_Success() throws Exception {
        // Arrange
        CustomerRequestDTO requestDTO = new CustomerRequestDTO();
        requestDTO.setPhoneNumber("1234567890");

        when(customerService.create(any(CustomerRequestDTO.class)))
            .thenReturn(testCustomerResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.customerId").value(1));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void testGetById_NotFound() throws Exception {
        // Arrange
        when(customerService.getById(999L))
            .thenThrow(new CustomerNotFoundException("Customer not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/999"))
                .andExpect(status().isNotFound());
    }
}
```

---

## 📊 Test Coverage Metrics

### Target Coverage:

- **Line Coverage:** 80%+ ✅
- **Branch Coverage:** 70%+ ✅
- **Method Coverage:** 90%+ ✅

### View Coverage Report:

1. Run: `mvn clean test jacoco:report`
2. Open: `target/site/jacoco/index.html`
3. Navigate to your service/controller classes
4. Green = Covered, Red = Not Covered

---

## 🐛 Common Issues and Solutions

### Issue 1: Mock Not Working

```java
// ❌ Wrong
@Mock
CustomerRepository repository;

// ✅ Correct
@Mock
private CustomerRepository repository;

@InjectMocks
private CustomerService service;
```

### Issue 2: NullPointerException in Tests

```java
// Ensure @BeforeEach initializes test data
@BeforeEach
void setUp() {
    testCustomer = new Customer();
    testCustomer.setCustomerId(1L);
    // ... initialize all required fields
}
```

### Issue 3: Verification Failures

```java
// ❌ Wrong - matcher mismatch
verify(repository).save(any());

// ✅ Correct - use consistent matchers
verify(repository).save(any(Customer.class));
```

---

## 📚 Additional Resources

### JUnit 5 Assertions

```java
assertEquals(expected, actual)
assertNotNull(object)
assertTrue(condition)
assertFalse(condition)
assertThrows(Exception.class, () -> method())
assertAll(
    () -> assertEquals(1, result.getId()),
    () -> assertEquals("Name", result.getName())
)
```

### Spring Boot Test Annotations

```java
@SpringBootTest              // Loads full application context
@WebMvcTest                  // Only web layer
@DataJpaTest                 // Only JPA components
@MockBean                    // Mock bean in Spring context
@Autowired                   // Inject real beans
@WithMockUser               // Mock authenticated user
@AutoConfigureMockMvc       // Configure MockMvc
```

---

## ✅ Completion Checklist

### Service Layer Unit Tests

- [x] CustomerService (33 tests)
- [x] VehicleService (20 tests)
- [x] AppointmentService (21 tests)
- [x] ProjectService (17 tests)
- [x] TaskService (15 tests)
- [x] TimeLogService (12 tests)
- [x] EmployeeService (21 tests)

### Controller Integration Tests

- [x] CustomerController (17 tests)
- [x] VehicleController (17 tests)
- [x] AppointmentController (20 tests)
- [x] ProjectController (18 tests)
- [x] TaskController (18 tests)
- [x] TimeLogController (17 tests)
- [x] EmployeeController (19 tests)

### GraphQL Tests

- [x] SearchGraphQLController (14 tests)
- [x] GraphQLHealthCheckController (5 tests)

---

## � GraphQL Testing Guide

### SearchGraphQLController Tests (14 tests)

**Tested Queries:**

- `searchAppointmentsByCustomerName` - Find appointments by customer name
- `searchCustomersByCustomerName` - Find customers by name
- `searchEmployeesByEmployeeName` - Find employees by name
- `searchTasksByTaskName` - Find tasks by name

**Test Coverage for Each Query:**

```java
@SpringBootTest
@SuppressWarnings("removal")
class SearchGraphQLControllerTest {

    @Autowired
    private SearchGraphQLController searchGraphQLController;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    void testSearchAppointmentsByCustomerName_Success() {
        // Arrange
        List<AppointmentSearchResponseDTO> appointments =
            Arrays.asList(testAppointment);
        when(appointmentService.searchAppointmentsByCustomerName("John"))
            .thenReturn(appointments);

        // Act
        List<AppointmentSearchResponseDTO> result =
            searchGraphQLController.searchAppointmentsByCustomerName("John");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentService, times(1))
            .searchAppointmentsByCustomerName("John");
    }

    @Test
    void testSearchAppointmentsByCustomerName_EmptyResult() {
        // Test when no results found
        when(appointmentService.searchAppointmentsByCustomerName("NonExistent"))
            .thenReturn(Arrays.asList());

        List<AppointmentSearchResponseDTO> result =
            searchGraphQLController.searchAppointmentsByCustomerName("NonExistent");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchAppointmentsByCustomerName_MultipleResults() {
        // Test when multiple results found
        List<AppointmentSearchResponseDTO> appointments =
            Arrays.asList(appointment1, appointment2);
        when(appointmentService.searchAppointmentsByCustomerName("John"))
            .thenReturn(appointments);

        List<AppointmentSearchResponseDTO> result =
            searchGraphQLController.searchAppointmentsByCustomerName("John");

        assertEquals(2, result.size());
    }
}
```

### GraphQLHealthCheckController Tests (5 tests)

**Tested Query:**

- `healthCheck` - Verify GraphQL endpoint is operational

**Test Coverage:**

```java
@SpringBootTest
class GraphQLHealthCheckControllerTest {

    @Autowired
    private GraphQLHealthCheckController graphQLHealthCheckController;

    @Test
    void testHealthCheck_Success() {
        String result = graphQLHealthCheckController.healthCheck();

        assertNotNull(result);
        assertEquals("GraphQL is working successfully", result);
    }

    @Test
    void testHealthCheck_ReturnsNonNullValue() {
        String result = graphQLHealthCheckController.healthCheck();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testHealthCheck_MultipleCalls_ReturnsSameResult() {
        // Test idempotency
        String result1 = graphQLHealthCheckController.healthCheck();
        String result2 = graphQLHealthCheckController.healthCheck();
        String result3 = graphQLHealthCheckController.healthCheck();

        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }
}
```

### GraphQL Test Patterns

**Pattern 1: Success Scenario**

```java
@Test
void testSearch_Success() {
    // Arrange - Mock service to return results
    when(service.searchMethod("query")).thenReturn(expectedResults);

    // Act - Call controller method
    List<ResponseDTO> result = controller.searchMethod("query");

    // Assert - Verify results and service interaction
    assertNotNull(result);
    assertEquals(expectedSize, result.size());
    verify(service, times(1)).searchMethod("query");
}
```

**Pattern 2: Empty Results**

```java
@Test
void testSearch_EmptyResult() {
    // Arrange - Mock service to return empty list
    when(service.searchMethod("nonexistent")).thenReturn(Arrays.asList());

    // Act
    List<ResponseDTO> result = controller.searchMethod("nonexistent");

    // Assert - Verify empty list returned
    assertNotNull(result);
    assertTrue(result.isEmpty());
}
```

**Pattern 3: Multiple Results**

```java
@Test
void testSearch_MultipleResults() {
    // Arrange - Mock service to return multiple items
    List<ResponseDTO> items = Arrays.asList(item1, item2, item3);
    when(service.searchMethod("query")).thenReturn(items);

    // Act
    List<ResponseDTO> result = controller.searchMethod("query");

    // Assert - Verify all items returned
    assertEquals(3, result.size());
    assertEquals(item1.getId(), result.get(0).getId());
    assertEquals(item2.getId(), result.get(1).getId());
}
```

---

## �🎓 Learning Outcomes

After completing this testing assignment, you will have learned:

1. ✅ **Unit Testing** - Testing business logic in isolation
2. ✅ **Mocking** - Using Mockito to mock dependencies
3. ✅ **Integration Testing** - Testing REST endpoints with MockMvc
4. ✅ **GraphQL Testing** - Testing GraphQL query endpoints
5. ✅ **Test Coverage** - Measuring and improving code coverage
6. ✅ **Spring Boot Testing** - Using Spring Boot test framework
7. ✅ **Best Practices** - Writing maintainable and readable tests

---

## 📞 Need Help?

### Common Commands Quick Reference

```powershell
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=CustomerServiceUnitTest

# Run all service unit tests
mvn test -Dtest="*ServiceUnitTest"

# Run all controller integration tests
mvn test -Dtest="*ControllerIntegrationTest"

# Run all GraphQL tests
mvn test -Dtest="*GraphQL*Test"

# Run with coverage
mvn clean test jacoco:report

# Skip tests (when building)
mvn clean install -DskipTests

# Run tests in debug mode
mvnDebug test -Dtest=CustomerServiceUnitTest
```

### Debugging Test Failures

1. Read the error message carefully
2. Check the stack trace
3. Verify mock setup in @BeforeEach
4. Ensure all required fields are initialized
5. Check argument matchers match
6. Verify the method is actually being called
