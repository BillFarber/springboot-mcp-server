/**
 * 🎸 Comprehensive MarkLogic Optic API Examples - Rush 2112 Style! 🎸
 * 
 * This file contains extensive examples of MarkLogic Optic functions with detailed
 * comments explaining how and when to use each function. Like Rush's intricate
 * compositions, these examples demonstrate the power and precision of Optic queries.
 * 
 * The Optic API provides a relational interface to MarkLogic, allowing you to:
 * - Query documents as rows and columns
 * - Join data from multiple sources
 * - Transform and aggregate data
 * - Build complex analytical queries
 * 
 * Author: SpringBoot MCP Server
 * Version: 1.0.0
 * Last Updated: 2025
 */

const op = require('/MarkLogic/optic');

// ============================================================================
// 🎸 BASIC QUERY OPERATIONS - The Foundation 🎸
// ============================================================================

/**
 * fromView() - Read data from a TDE view
 * Use when: You have Template Driven Extraction (TDE) views defined and want to query structured data
 * Best for: Well-defined data schemas with consistent structure
 */
const basicViewQuery = op.fromView('employees', 'employee_details')
  .result();
// Returns all rows from the employee_details view in the employees schema

/**
 * fromTriples() - Query RDF triples
 * Use when: Working with semantic/RDF data
 * Best for: Knowledge graphs, linked data, semantic applications
 */
const tripleQuery = op.fromTriples([
  op.pattern(op.col('subject'), op.sem.iri('http://example.org/name'), op.col('name')),
  op.pattern(op.col('subject'), op.sem.iri('http://example.org/age'), op.col('age'))
])
.result();

/**
 * fromLexicons() - Query range indexes directly
 * Use when: You need fast access to indexed values without full document retrieval
 * Best for: Analytics on specific fields, aggregations, filtering on indexed values
 */
const lexiconQuery = op.fromLexicons({
  'uri': cts.uriReference(),
  'timestamp': cts.elementReference(xs.QName('timestamp')),
  'category': cts.elementReference(xs.QName('category'))
})
.result();

// ============================================================================
// 🎸 SELECTION AND PROJECTION - Choose Your Columns 🎸
// ============================================================================

/**
 * select() - Choose specific columns
 * Use when: You only need specific fields from your data
 * Best for: Reducing data transfer, focusing on relevant information
 */
const selectColumns = op.fromView('products', 'catalog')
  .select(['product_id', 'name', 'price', 'category'])
  .result();

/**
 * selectExcept() - Select all columns except specified ones
 * Use when: You want most columns but need to exclude a few
 * Best for: Removing sensitive data, excluding large binary fields
 */
const selectExceptSensitive = op.fromView('users', 'profiles')
  .selectExcept(['password_hash', 'ssn', 'credit_card'])
  .result();

/**
 * as() - Rename columns with aliases
 * Use when: You need different column names in results
 * Best for: Making output more readable, avoiding naming conflicts
 */
const aliasedColumns = op.fromView('sales', 'transactions')
  .select([
    op.as('txn_id', op.col('transaction_id')),
    op.as('customer', op.col('customer_name')),
    op.as('total_amount', op.col('amount')),
    op.as('sale_date', op.col('transaction_date'))
  ])
  .result();

// ============================================================================
// 🎸 FILTERING AND CONDITIONS - Where the Magic Happens 🎸
// ============================================================================

/**
 * where() - Filter rows based on conditions
 * Use when: You need to filter data based on specific criteria
 * Best for: Finding specific records, applying business rules
 */
const filteredData = op.fromView('orders', 'order_details')
  .where(op.and(
    op.ge(op.col('order_date'), '2024-01-01'),
    op.eq(op.col('status'), 'completed'),
    op.gt(op.col('total_amount'), 100)
  ))
  .result();

/**
 * Complex filtering with multiple conditions
 * Use when: You have complex business logic for filtering
 * Best for: Advanced search functionality, complex reporting
 */
const complexFilter = op.fromView('employees', 'staff')
  .where(op.or(
    op.and(
      op.eq(op.col('department'), 'Engineering'),
      op.ge(op.col('salary'), 80000)
    ),
    op.and(
      op.eq(op.col('department'), 'Sales'),
      op.ge(op.col('commission_rate'), 0.15)
    ),
    op.eq(op.col('level'), 'Senior')
  ))
  .result();

/**
 * whereDistinct() - Filter and ensure uniqueness
 * Use when: You want unique results based on specific columns
 * Best for: Removing duplicates while filtering
 */
const distinctFiltered = op.fromView('customers', 'contacts')
  .whereDistinct(op.ne(op.col('email'), ''))
  .result();

// ============================================================================
// 🎸 JOINING DATA - Bringing It All Together 🎸
// ============================================================================

/**
 * joinInner() - Inner join between datasets
 * Use when: You only want records that exist in both datasets
 * Best for: Guaranteed matches, referential integrity scenarios
 */
const innerJoinExample = op.fromView('orders', 'order_summary')
  .joinInner(
    op.fromView('customers', 'customer_info'),
    op.on(op.col('customer_id'), op.col('id'))
  )
  .select([
    'order_id',
    'order_date',
    'customer_name',
    'customer_email',
    'total_amount'
  ])
  .result();

/**
 * joinLeftOuter() - Left outer join
 * Use when: You want all records from the left side, even without matches
 * Best for: Optional relationships, reporting with missing data
 */
const leftJoinExample = op.fromView('products', 'inventory')
  .joinLeftOuter(
    op.fromView('sales', 'product_sales'),
    op.on(op.col('product_id'), op.col('product_id'))
  )
  .select([
    'product_name',
    'current_stock',
    op.as('total_sold', op.col('quantity_sold')),
    op.as('revenue', op.col('total_revenue'))
  ])
  .result();

/**
 * joinDoc() - Join with document content
 * Use when: You need to access full document content along with structured data
 * Best for: Mixing structured queries with document retrieval
 */
const docJoinExample = op.fromView('articles', 'metadata')
  .joinDoc(op.col('doc'), op.col('uri'))
  .select([
    'title',
    'author',
    'publish_date',
    op.as('content', op.xpath('doc', '//content/text()'))
  ])
  .result();

/**
 * Multiple joins with complex relationships
 * Use when: You need data from multiple related tables
 * Best for: Complex reporting, data warehousing scenarios
 */
const multipleJoins = op.fromView('orders', 'transactions')
  .joinInner(
    op.fromView('customers', 'profiles'),
    op.on(op.col('customer_id'), op.col('customer_id'))
  )
  .joinInner(
    op.fromView('products', 'catalog'),
    op.on(op.col('product_id'), op.col('product_id'))
  )
  .joinLeftOuter(
    op.fromView('promotions', 'discounts'),
    op.on(op.col('promo_code'), op.col('code'))
  )
  .select([
    'order_id',
    'customer_name',
    'product_name',
    'quantity',
    'unit_price',
    op.as('discount_percent', op.col('discount_rate'))
  ])
  .result();

// ============================================================================
// 🎸 AGGREGATION - The Power of Numbers 🎸
// ============================================================================

/**
 * groupBy() with aggregation functions
 * Use when: You need summary statistics grouped by categories
 * Best for: Reports, dashboards, analytical queries
 */
const groupedAggregation = op.fromView('sales', 'daily_transactions')
  .groupBy([
    'region',
    'product_category'
  ], [
    op.as('total_sales', op.sum(op.col('amount'))),
    op.as('avg_order_value', op.avg(op.col('amount'))),
    op.as('transaction_count', op.count()),
    op.as('max_single_sale', op.max(op.col('amount'))),
    op.as('min_single_sale', op.min(op.col('amount')))
  ])
  .result();

/**
 * Advanced aggregation with calculated fields
 * Use when: You need complex calculations in your aggregations
 * Best for: Business intelligence, financial reporting
 */
const advancedAggregation = op.fromView('employees', 'payroll')
  .groupBy(['department', 'job_level'], [
    op.as('employee_count', op.count()),
    op.as('total_salary_cost', op.sum(op.col('annual_salary'))),
    op.as('avg_salary', op.avg(op.col('annual_salary'))),
    op.as('salary_range', op.subtract(op.max(op.col('annual_salary')), op.min(op.col('annual_salary')))),
    op.as('total_benefits_cost', op.sum(op.multiply(op.col('annual_salary'), 0.25)))
  ])
  .orderBy(['department', op.desc('avg_salary')])
  .result();

/**
 * Aggregation without groupBy() - overall totals
 * Use when: You need overall statistics across all data
 * Best for: Summary dashboards, KPI calculations
 */
const overallStats = op.fromView('website', 'page_views')
  .where(op.ge(op.col('view_date'), '2024-01-01'))
  .select([
    op.as('total_page_views', op.sum(op.col('view_count'))),
    op.as('unique_visitors', op.countDistinct(op.col('visitor_id'))),
    op.as('avg_session_duration', op.avg(op.col('session_duration'))),
    op.as('bounce_rate', op.divide(
      op.sum(op.when(op.eq(op.col('page_count'), 1), 1, 0)),
      op.count()
    ))
  ])
  .result();

// ============================================================================
// 🎸 SORTING AND ORDERING - Arrange with Precision 🎸
// ============================================================================

/**
 * orderBy() - Sort results
 * Use when: You need results in a specific order
 * Best for: Reports, user interfaces, ranked lists
 */
const sortedResults = op.fromView('products', 'bestsellers')
  .orderBy([
    op.desc('total_sales'),
    'product_name',
    op.asc('price')
  ])
  .result();

/**
 * Complex sorting with calculated fields
 * Use when: You need to sort by computed values
 * Best for: Custom rankings, score-based sorting
 */
const customSortedResults = op.fromView('students', 'grades')
  .select([
    'student_name',
    'subject',
    op.as('weighted_score', op.add(
      op.multiply(op.col('homework_avg'), 0.3),
      op.multiply(op.col('test_avg'), 0.4),
      op.multiply(op.col('final_exam'), 0.3)
    ))
  ])
  .orderBy([
    'subject',
    op.desc('weighted_score'),
    'student_name'
  ])
  .result();

// ============================================================================
// 🎸 MATHEMATICAL OPERATIONS - Calculate Like a Rock Star 🎸
// ============================================================================

/**
 * Mathematical functions in select
 * Use when: You need calculated fields in your results
 * Best for: Financial calculations, scientific computations, metrics
 */
const mathematicalOperations = op.fromView('financial', 'investments')
  .select([
    'account_id',
    'principal_amount',
    'interest_rate',
    'years',
    // Simple interest calculation
    op.as('simple_interest', op.multiply(
      op.col('principal_amount'),
      op.col('interest_rate'),
      op.col('years')
    )),
    // Compound interest calculation (approximation)
    op.as('compound_interest', op.subtract(
      op.multiply(
        op.col('principal_amount'),
        op.power(op.add(1, op.col('interest_rate')), op.col('years'))
      ),
      op.col('principal_amount')
    )),
    // Percentage calculations
    op.as('growth_percentage', op.multiply(
      op.divide(
        op.subtract(op.col('current_value'), op.col('principal_amount')),
        op.col('principal_amount')
      ),
      100
    ))
  ])
  .result();

/**
 * Statistical functions
 * Use when: You need statistical analysis of your data
 * Best for: Data analysis, quality metrics, performance monitoring
 */
const statisticalAnalysis = op.fromView('performance', 'response_times')
  .groupBy(['service_name'], [
    op.as('avg_response_time', op.avg(op.col('response_ms'))),
    op.as('median_response_time', op.median(op.col('response_ms'))),
    op.as('std_deviation', op.stddev(op.col('response_ms'))),
    op.as('variance', op.variance(op.col('response_ms'))),
    op.as('p95_response_time', op.percentile(op.col('response_ms'), 95)),
    op.as('p99_response_time', op.percentile(op.col('response_ms'), 99))
  ])
  .result();

// ============================================================================
// 🎸 STRING OPERATIONS - Text Manipulation Mastery 🎸
// ============================================================================

/**
 * String functions for text processing
 * Use when: You need to manipulate or analyze text data
 * Best for: Data cleaning, formatting, text analysis
 */
const stringOperations = op.fromView('users', 'profiles')
  .select([
    'user_id',
    op.as('full_name', op.concat(op.col('first_name'), ' ', op.col('last_name'))),
    op.as('name_length', op.length(op.concat(op.col('first_name'), op.col('last_name')))),
    op.as('email_domain', op.substring(op.col('email'), op.add(op.indexOf(op.col('email'), '@'), 1))),
    op.as('first_initial', op.substring(op.col('first_name'), 1, 1)),
    op.as('username_upper', op.upper(op.col('username'))),
    op.as('display_name', op.lower(op.replace(op.col('display_name'), ' ', '_'))),
    op.as('phone_cleaned', op.replace(op.replace(op.col('phone'), '(', ''), ')', ''))
  ])
  .result();

/**
 * Pattern matching and text search
 * Use when: You need to find or validate text patterns
 * Best for: Data validation, search functionality, pattern detection
 */
const patternMatching = op.fromView('documents', 'content')
  .where(op.and(
    op.like(op.col('title'), '%Financial%'),
    op.matches(op.col('document_id'), '^[A-Z]{3}[0-9]{4}$'),
    op.contains(op.col('content'), 'quarterly report')
  ))
  .select([
    'document_id',
    'title',
    op.as('title_word_count', op.length(op.tokenize(op.col('title')))),
    op.as('has_summary', op.isDefined(op.col('summary')))
  ])
  .result();

// ============================================================================
// 🎸 DATE AND TIME OPERATIONS - Temporal Precision 🎸
// ============================================================================

/**
 * Date/time functions and calculations
 * Use when: You need to work with temporal data
 * Best for: Time-based analysis, scheduling, historical reporting
 */
const dateTimeOperations = op.fromView('events', 'calendar')
  .select([
    'event_id',
    'event_name',
    'start_time',
    'end_time',
    // Extract date components
    op.as('year', op.year(op.col('start_time'))),
    op.as('month', op.month(op.col('start_time'))),
    op.as('day_of_week', op.dayOfWeek(op.col('start_time'))),
    op.as('quarter', op.quarter(op.col('start_time'))),
    // Calculate durations
    op.as('duration_minutes', op.dateDiff('minute', op.col('start_time'), op.col('end_time'))),
    op.as('days_until_event', op.dateDiff('day', op.currentDateTime(), op.col('start_time'))),
    // Format dates
    op.as('formatted_date', op.formatDateTime(op.col('start_time'), 'YYYY-MM-DD')),
    op.as('formatted_time', op.formatDateTime(op.col('start_time'), 'HH:mm:ss'))
  ])
  .where(op.ge(op.col('start_time'), op.currentDate()))
  .result();

/**
 * Time-based filtering and grouping
 * Use when: You need time-based analysis or reporting
 * Best for: Trend analysis, periodic reports, time series data
 */
const timeBasedAnalysis = op.fromView('sales', 'transactions')
  .where(op.and(
    op.ge(op.col('transaction_date'), op.subtract(op.currentDate(), op.duration('P1Y'))),
    op.lt(op.col('transaction_date'), op.currentDate())
  ))
  .groupBy([
    op.as('year_month', op.formatDateTime(op.col('transaction_date'), 'YYYY-MM'))
  ], [
    op.as('monthly_revenue', op.sum(op.col('amount'))),
    op.as('transaction_count', op.count()),
    op.as('avg_transaction_value', op.avg(op.col('amount'))),
    op.as('first_transaction_date', op.min(op.col('transaction_date'))),
    op.as('last_transaction_date', op.max(op.col('transaction_date')))
  ])
  .orderBy(['year_month'])
  .result();

// ============================================================================
// 🎸 CONDITIONAL LOGIC - Decision Making in Queries 🎸
// ============================================================================

/**
 * Case expressions for conditional logic
 * Use when: You need if-then-else logic in your queries
 * Best for: Data categorization, conditional calculations, business rules
 */
const conditionalLogic = op.fromView('customers', 'profiles')
  .select([
    'customer_id',
    'customer_name',
    'total_purchases',
    'registration_date',
    // Customer tier based on purchase amount
    op.as('customer_tier', 
      op.case([
        op.when(op.ge(op.col('total_purchases'), 10000), 'Platinum'),
        op.when(op.ge(op.col('total_purchases'), 5000), 'Gold'),
        op.when(op.ge(op.col('total_purchases'), 1000), 'Silver')
      ], 'Bronze')
    ),
    // Loyalty status based on registration date
    op.as('loyalty_status',
      op.case([
        op.when(op.le(op.col('registration_date'), op.subtract(op.currentDate(), op.duration('P5Y'))), 'Veteran'),
        op.when(op.le(op.col('registration_date'), op.subtract(op.currentDate(), op.duration('P2Y'))), 'Established'),
        op.when(op.le(op.col('registration_date'), op.subtract(op.currentDate(), op.duration('P1Y'))), 'Regular')
      ], 'New')
    ),
    // Discount rate calculation
    op.as('discount_rate',
      op.case([
        op.when(op.ge(op.col('total_purchases'), 10000), 0.15),
        op.when(op.ge(op.col('total_purchases'), 5000), 0.10),
        op.when(op.ge(op.col('total_purchases'), 1000), 0.05)
      ], 0.00)
    )
  ])
  .result();

/**
 * Null handling and coalescing
 * Use when: You need to handle missing or null values gracefully
 * Best for: Data cleaning, default values, defensive programming
 */
const nullHandling = op.fromView('products', 'inventory')
  .select([
    'product_id',
    'product_name',
    // Handle null descriptions
    op.as('description', op.coalesce(op.col('description'), 'No description available')),
    // Handle null prices with defaults
    op.as('display_price', op.coalesce(op.col('sale_price'), op.col('regular_price'), 0)),
    // Handle null categories
    op.as('category', op.coalesce(op.col('category'), 'Uncategorized')),
    // Check for missing values
    op.as('has_image', op.not(op.isNull(op.col('image_url')))),
    op.as('is_complete_profile', op.and(
      op.not(op.isNull(op.col('description'))),
      op.not(op.isNull(op.col('category'))),
      op.not(op.isNull(op.col('image_url')))
    ))
  ])
  .result();

// ============================================================================
// 🎸 LIMITING AND PAGINATION - Control Your Results 🎸
// ============================================================================

/**
 * limit() and offset() for pagination
 * Use when: You need to implement pagination or limit result sets
 * Best for: Web applications, large datasets, performance optimization
 */
const paginatedResults = op.fromView('articles', 'blog_posts')
  .where(op.eq(op.col('status'), 'published'))
  .orderBy([op.desc('publish_date')])
  .offset(20)  // Skip first 20 results (page 3 if 10 per page)
  .limit(10)   // Return next 10 results
  .result();

/**
 * Top N queries with limit
 * Use when: You only want the best/worst/latest N records
 * Best for: Leaderboards, recent activity, top performers
 */
const topPerformers = op.fromView('sales', 'representative_performance')
  .where(op.and(
    op.ge(op.col('period'), '2024-01-01'),
    op.lt(op.col('period'), '2025-01-01')
  ))
  .groupBy(['rep_id', 'rep_name'], [
    op.as('total_sales', op.sum(op.col('sales_amount'))),
    op.as('deals_closed', op.count())
  ])
  .orderBy([op.desc('total_sales')])
  .limit(10)  // Top 10 sales reps
  .result();

// ============================================================================
// 🎸 WINDOW FUNCTIONS - Advanced Analytics 🎸
// ============================================================================

/**
 * Window functions for advanced analytics
 * Use when: You need running totals, rankings, or row-by-row comparisons
 * Best for: Financial analysis, trend detection, competitive analysis
 */
const windowFunctions = op.fromView('sales', 'monthly_revenue')
  .select([
    'month',
    'region',
    'revenue',
    // Running total within each region
    op.as('running_total', op.sum(op.col('revenue')).over(
      op.partitionBy('region').orderBy('month')
    )),
    // Rank within region by revenue
    op.as('revenue_rank', op.rank().over(
      op.partitionBy('region').orderBy(op.desc('revenue'))
    )),
    // Moving average (3-month window)
    op.as('moving_avg_3m', op.avg(op.col('revenue')).over(
      op.partitionBy('region').orderBy('month').rows(2).preceding()
    )),
    // Previous month's revenue for comparison
    op.as('prev_month_revenue', op.lag(op.col('revenue'), 1).over(
      op.partitionBy('region').orderBy('month')
    )),
    // Percentage change from previous month
    op.as('month_over_month_pct', op.multiply(
      op.divide(
        op.subtract(op.col('revenue'), op.lag(op.col('revenue'), 1).over(
          op.partitionBy('region').orderBy('month')
        )),
        op.lag(op.col('revenue'), 1).over(
          op.partitionBy('region').orderBy('month')
        )
      ),
      100
    ))
  ])
  .result();

// ============================================================================
// 🎸 SUBQUERIES AND COMPLEX OPERATIONS - Epic Compositions 🎸
// ============================================================================

/**
 * Subqueries for complex filtering
 * Use when: You need to filter based on aggregated or calculated values
 * Best for: Finding above-average performers, outlier detection
 */
const subqueryExample = op.fromView('employees', 'salaries')
  .where(op.gt(
    op.col('salary'),
    op.fromView('employees', 'salaries')
      .select([op.avg(op.col('salary'))])
      .result()[0].avg_salary  // This would need to be handled differently in practice
  ))
  .select([
    'employee_id',
    'name',
    'department',
    'salary',
    op.as('above_avg_by', op.subtract(
      op.col('salary'),
      75000  // Would be the calculated average
    ))
  ])
  .result();

/**
 * EXISTS and NOT EXISTS patterns
 * Use when: You need to check for the existence of related records
 * Best for: Finding orphaned records, checking relationships
 */
const existsExample = op.fromView('customers', 'profiles')
  .where(op.exists(
    op.fromView('orders', 'transactions')
      .where(op.and(
        op.eq(op.col('customer_id'), op.viewCol('customers', 'customer_id')),
        op.ge(op.col('order_date'), '2024-01-01')
      ))
  ))
  .select(['customer_id', 'customer_name', 'registration_date'])
  .result();

// ============================================================================
// 🎸 UNION AND COMBINATION OPERATIONS - Bringing Data Together 🎸
// ============================================================================

/**
 * union() to combine multiple datasets
 * Use when: You need to combine similar data from different sources
 * Best for: Consolidating data, combining historical with current data
 */
const unionExample = op.fromView('sales', 'current_year')
  .select(['order_id', 'customer_id', 'amount', 'order_date'])
  .union(
    op.fromView('sales', 'previous_year')
      .select(['order_id', 'customer_id', 'amount', 'order_date'])
  )
  .orderBy(['order_date'])
  .result();

/**
 * unionAll() to combine with duplicates
 * Use when: You want to preserve all records including duplicates
 * Best for: Audit trails, complete historical records
 */
const unionAllExample = op.fromView('logs', 'error_log_2024')
  .select(['timestamp', 'error_level', 'message'])
  .unionAll(
    op.fromView('logs', 'error_log_2023')
      .select(['timestamp', 'error_level', 'message'])
  )
  .where(op.eq(op.col('error_level'), 'ERROR'))
  .orderBy([op.desc('timestamp')])
  .result();

// ============================================================================
// 🎸 XML AND DOCUMENT PROCESSING - Handle All Data Types 🎸
// ============================================================================

/**
 * Working with XML content in documents
 * Use when: You have XML documents and need to extract specific elements
 * Best for: Document processing, content extraction, metadata analysis
 */
const xmlProcessing = op.fromView('documents', 'xml_docs')
  .joinDoc(op.col('doc'), op.col('uri'))
  .select([
    'doc_id',
    op.as('title', op.xpath('doc', '//title/text()')),
    op.as('author', op.xpath('doc', '//author/@name')),
    op.as('chapter_count', op.xpath('doc', 'count(//chapter)')),
    op.as('word_count', op.xpath('doc', 'string-length(//content)')),
    op.as('last_modified', op.xpath('doc', '//metadata/last-modified/text()'))
  ])
  .where(op.not(op.isNull(op.xpath('doc', '//title'))))
  .result();

/**
 * JSON document processing
 * Use when: You have JSON documents and need to extract properties
 * Best for: NoSQL-style queries, API data processing, flexible schemas
 */
const jsonProcessing = op.fromView('api_data', 'responses')
  .joinDoc(op.col('doc'), op.col('uri'))
  .select([
    'request_id',
    op.as('response_code', op.jsonPropertyValue('doc', 'status.code')),
    op.as('response_message', op.jsonPropertyValue('doc', 'status.message')),
    op.as('data_count', op.jsonPropertyValue('doc', 'data.length')),
    op.as('processing_time', op.jsonPropertyValue('doc', 'metadata.processing_time_ms')),
    op.as('has_errors', op.jsonExists('doc', 'errors'))
  ])
  .where(op.eq(op.jsonPropertyValue('doc', 'status.code'), 200))
  .result();

// ============================================================================
// 🎸 PERFORMANCE OPTIMIZATION - Rock Star Efficiency 🎸
// ============================================================================

/**
 * Optimized query with proper indexing hints
 * Use when: You need to optimize query performance
 * Best for: Large datasets, production systems, real-time applications
 */
const optimizedQuery = op.fromView('transactions', 'high_volume')
  // Use range indexes for efficient filtering
  .where(op.and(
    op.range(op.col('transaction_date'), '>=', '2024-01-01'),
    op.range(op.col('transaction_date'), '<', '2025-01-01'),
    op.range(op.col('amount'), '>', 1000)
  ))
  // Select only needed columns to reduce I/O
  .select([
    'transaction_id',
    'customer_id',
    'amount',
    'transaction_date'
  ])
  // Efficient sorting on indexed columns
  .orderBy(['transaction_date', 'customer_id'])
  // Limit results to control memory usage
  .limit(1000)
  .result();

/**
 * Batch processing pattern for large datasets
 * Use when: You need to process large amounts of data efficiently
 * Best for: ETL operations, bulk updates, data migration
 */
const batchProcessing = (batchSize = 1000, offsetValue = 0) => {
  return op.fromView('large_dataset', 'records')
    .where(op.gt(op.col('id'), offsetValue))
    .orderBy(['id'])
    .limit(batchSize)
    .select([
      'id',
      'data_field',
      'processed_flag'
    ])
    .result();
};

// ============================================================================
// 🎸 REAL-WORLD EXAMPLES - Epic Business Scenarios 🎸
// ============================================================================

/**
 * Customer Analytics Dashboard Query
 * Use case: Generate comprehensive customer insights for business dashboard
 */
const customerAnalytics = op.fromView('customers', 'profiles')
  .joinLeftOuter(
    op.fromView('orders', 'summary')
      .groupBy(['customer_id'], [
        op.as('total_orders', op.count()),
        op.as('total_spent', op.sum(op.col('amount'))),
        op.as('avg_order_value', op.avg(op.col('amount'))),
        op.as('first_order_date', op.min(op.col('order_date'))),
        op.as('last_order_date', op.max(op.col('order_date')))
      ]),
    op.on(op.col('customer_id'), op.col('customer_id'))
  )
  .select([
    'customer_id',
    'customer_name',
    'email',
    'registration_date',
    op.as('orders_count', op.coalesce(op.col('total_orders'), 0)),
    op.as('lifetime_value', op.coalesce(op.col('total_spent'), 0)),
    op.as('average_order_value', op.coalesce(op.col('avg_order_value'), 0)),
    op.as('customer_segment',
      op.case([
        op.when(op.ge(op.coalesce(op.col('total_spent'), 0), 5000), 'VIP'),
        op.when(op.ge(op.coalesce(op.col('total_spent'), 0), 1000), 'Premium'),
        op.when(op.gt(op.coalesce(op.col('total_orders'), 0), 0), 'Active')
      ], 'Inactive')
    ),
    op.as('days_since_last_order', op.dateDiff('day', op.col('last_order_date'), op.currentDate())),
    op.as('customer_tenure_days', op.dateDiff('day', op.col('registration_date'), op.currentDate()))
  ])
  .orderBy([op.desc('lifetime_value')])
  .result();

/**
 * Financial Reporting Query
 * Use case: Generate monthly financial reports with year-over-year comparisons
 */
const financialReporting = op.fromView('transactions', 'financial')
  .where(op.ge(op.col('transaction_date'), '2023-01-01'))
  .groupBy([
    op.as('year', op.year(op.col('transaction_date'))),
    op.as('month', op.month(op.col('transaction_date'))),
    'department'
  ], [
    op.as('monthly_revenue', op.sum(
      op.when(op.eq(op.col('transaction_type'), 'revenue'), op.col('amount'), 0)
    )),
    op.as('monthly_expenses', op.sum(
      op.when(op.eq(op.col('transaction_type'), 'expense'), op.col('amount'), 0)
    )),
    op.as('net_income', op.subtract(
      op.sum(op.when(op.eq(op.col('transaction_type'), 'revenue'), op.col('amount'), 0)),
      op.sum(op.when(op.eq(op.col('transaction_type'), 'expense'), op.col('amount'), 0))
    )),
    op.as('transaction_count', op.count())
  ])
  .select([
    'year',
    'month',
    'department',
    'monthly_revenue',
    'monthly_expenses',
    'net_income',
    op.as('profit_margin', op.multiply(
      op.divide(op.col('net_income'), op.col('monthly_revenue')),
      100
    )),
    // Year-over-year growth calculation would require additional subqueries
    op.as('reporting_period', op.concat(op.col('year'), '-', 
      op.case([
        op.when(op.lt(op.col('month'), 10), op.concat('0', op.col('month')))
      ], op.col('month'))
    ))
  ])
  .orderBy(['year', 'month', 'department'])
  .result();

/**
 * Inventory Management Query
 * Use case: Track inventory levels, reorder points, and supplier performance
 */
const inventoryManagement = op.fromView('inventory', 'current_stock')
  .joinInner(
    op.fromView('products', 'catalog'),
    op.on(op.col('product_id'), op.col('product_id'))
  )
  .joinLeftOuter(
    op.fromView('suppliers', 'information'),
    op.on(op.col('supplier_id'), op.col('supplier_id'))
  )
  .joinLeftOuter(
    op.fromView('sales', 'velocity')
      .groupBy(['product_id'], [
        op.as('avg_daily_sales', op.avg(op.col('daily_quantity_sold'))),
        op.as('max_daily_sales', op.max(op.col('daily_quantity_sold')))
      ]),
    op.on(op.col('product_id'), op.col('product_id'))
  )
  .select([
    'product_id',
    'product_name',
    'current_stock',
    'reorder_point',
    'reorder_quantity',
    'supplier_name',
    'lead_time_days',
    op.as('avg_daily_sales', op.coalesce(op.col('avg_daily_sales'), 0)),
    op.as('days_of_stock_remaining', op.divide(
      op.col('current_stock'),
      op.greatest(op.coalesce(op.col('avg_daily_sales'), 1), 1)
    )),
    op.as('reorder_status',
      op.case([
        op.when(op.le(op.col('current_stock'), op.col('reorder_point')), 'URGENT - Reorder Now'),
        op.when(op.le(op.col('current_stock'), op.multiply(op.col('reorder_point'), 1.2)), 'Warning - Low Stock'),
        op.when(op.ge(op.col('current_stock'), op.multiply(op.col('reorder_point'), 3)), 'Overstocked')
      ], 'Normal')
    ),
    op.as('estimated_stockout_date', 
      op.when(op.gt(op.coalesce(op.col('avg_daily_sales'), 0), 0),
        op.addDays(op.currentDate(), op.col('days_of_stock_remaining')),
        null
      )
    )
  ])
  .orderBy([
    op.case([
      op.when(op.eq(op.col('reorder_status'), 'URGENT - Reorder Now'), 1),
      op.when(op.eq(op.col('reorder_status'), 'Warning - Low Stock'), 2),
      op.when(op.eq(op.col('reorder_status'), 'Overstocked'), 4)
    ], 3),
    'days_of_stock_remaining'
  ])
  .result();

// ============================================================================
// 🎸 DEBUGGING AND TROUBLESHOOTING - Rock Star Debugging 🎸
// ============================================================================

/**
 * Query execution plan and debugging
 * Use when: You need to understand query performance or troubleshoot issues
 * Best for: Performance tuning, query optimization, debugging
 */
const debugQuery = op.fromView('large_table', 'data')
  .where(op.eq(op.col('status'), 'active'))
  .select(['id', 'name', 'value'])
  .orderBy(['name'])
  .explain();  // Get execution plan instead of results

/**
 * Error handling patterns
 * Use when: You need robust error handling in your queries
 * Best for: Production systems, data quality checks, defensive programming
 */
const errorHandlingQuery = (() => {
  try {
    return op.fromView('possibly_missing_view', 'data')
      .select([
        'id',
        op.as('safe_division', 
          op.when(op.ne(op.col('denominator'), 0),
            op.divide(op.col('numerator'), op.col('denominator')),
            null
          )
        ),
        op.as('validated_email',
          op.when(op.matches(op.col('email'), '^[^@]+@[^@]+\.[^@]+$'),
            op.col('email'),
            'INVALID_EMAIL'
          )
        )
      ])
      .result();
  } catch (error) {
    console.log('Query failed:', error.message);
    return [];
  }
})();

// ============================================================================
// 🎸 CONCLUSION - The Final Solo 🎸
// ============================================================================

/**
 * This comprehensive collection of Optic examples demonstrates the full power
 * and versatility of MarkLogic's Optic API. Like Rush's complex musical
 * arrangements, these queries can be combined and modified to create
 * sophisticated data processing pipelines.
 * 
 * Key takeaways:
 * 1. Start simple with basic queries and build complexity gradually
 * 2. Use appropriate functions for your data types (strings, dates, numbers)
 * 3. Leverage joins to combine data from multiple sources
 * 4. Use aggregation for analytical queries and reporting
 * 5. Apply filtering and sorting for specific business requirements
 * 6. Consider performance implications for large datasets
 * 7. Handle errors and edge cases gracefully
 * 
 * Remember: The best queries are like the best Rush songs - they accomplish
 * their purpose with precision, elegance, and just the right amount of complexity.
 * 
 * 🎸 Keep on rockin' with MarkLogic Optic! 🎸
 */

module.exports = {
  // Export key examples for reuse
  basicViewQuery,
  customerAnalytics,
  financialReporting,
  inventoryManagement,
  optimizedQuery,
  batchProcessing
};
