# Paperless-ngx REST API Reference

## Overview
Paperless-ngx provides a fully-documented REST API with a browsable web interface at `/api/schema/view/`. This reference captures the key endpoints and patterns needed for our pipeline implementation.

## Authentication Methods

### 1. Token Authentication (Recommended for Pipeline)
**Primary method for our headless service:**
```http
Authorization: Token <token>
```

**Token Acquisition:**
- Manual: Via "My Profile" in web UI
- Programmatic: POST to `/api/token/` with username/password
- Management: Via Django admin interface

### 2. Basic Authentication
```http
Authorization: Basic <base64(username:password)>
```

### 3. Session Authentication
Used when logged into web browser (not applicable for headless service).

### 4. Remote User Authentication
Enterprise authentication method (requires `PAPERLESS_ENABLE_HTTP_REMOTE_USER_API`).

## Core Endpoints for Pipeline

### Document Search & Retrieval
**Primary endpoint:** `/api/documents/`

**Search Parameters:**
- `?query=search%20terms` - Full text search
- `?more_like_id=1234` - Similar documents
- `?custom_field_query=...` - Custom field filtering

**Search Response Format:**
```json
{
    "count": 31,
    "next": "...",
    "previous": null,
    "results": [{
        "id": 123,
        "title": "title",
        "content": "content",
        "__search_hit__": {
            "score": 0.343,
            "highlights": "text <span class=\"match\">Test</span> text",
            "rank": 23
        }
    }]
}
```

### Document Upload
**Endpoint:** `/api/documents/post_document/`
**Method:** POST multipart form

**Form Fields:**
- `document` (required) - The document file
- `title` - Document title
- `created` - Creation date ("2016-04-19" or "2016-04-19 06:15:00+02:00")
- `correspondent` - Correspondent ID
- `document_type` - Document type ID
- `storage_path` - Storage path ID
- `tags` - Tag IDs (multiple values)
- `archive_serial_number` - Optional serial number
- `custom_fields` - Array of field IDs or object mapping field ID → value

**Response:** HTTP 200 with consumption task UUID

### Task Status Tracking
**Endpoint:** `/api/tasks/?task_id={uuid}`
Monitor document consumption progress and get created document ID.

## Custom Field Filtering

Critical for our idempotency and pipeline versioning:

### Filter Examples
1. **Date range filtering:**
   ```
   ?custom_field_query=["due", "range", ["2024-08-01", "2024-09-01"]]
   ```

2. **Exact text matching:**
   ```
   ?custom_field_query=["customer", "exact", "bob"]
   ```

3. **Boolean field filtering:**
   ```
   ?custom_field_query=["answered", "exact", true]
   ```

4. **Multiple values:**
   ```
   ?custom_field_query=["status", "in", ["pending", "processing"]]
   ```

5. **Empty field check:**
   ```
   ?custom_field_query=["OR", [["address", "isnull", true], ["address", "exact", ""]]]
   ```

6. **Field existence:**
   ```
   ?custom_field_query=["foo", "exists", false]
   ```

### Supported Operations by Field Type
- **All types:** `exact`, `in`, `isnull`, `exists`
- **String/URL/Monetary:** `icontains`, `istartswith`, `iendswith`
- **Integer/Float/Date:** `gt`, `gte`, `lt`, `lte`, `range`
- **Document Link:** `contains` (superset check)

## Bulk Operations

### Bulk Document Editing
**Endpoint:** `/api/documents/bulk_edit/`

**Request Format:**
```json
{
  "documents": [LIST_OF_DOCUMENT_IDS],
  "method": "METHOD_NAME",
  "parameters": { ... }
}
```

**Key Methods for Pipeline:**
- `add_tag` - Add processing status tags
- `remove_tag` - Remove queue tags
- `modify_tags` - Batch tag operations
- `set_correspondent` - Update correspondent
- `set_document_type` - Update document type
- `modify_custom_fields` - Update pipeline metadata
- `reprocess` - Trigger reprocessing

**Custom Fields Modification:**
```json
{
  "method": "modify_custom_fields",
  "parameters": {
    "add_custom_fields": {
      "FIELD_ID": "value",
      "PIPELINE_VERSION_FIELD": "v1.2.0",
      "DOCUMENT_HASH_FIELD": "sha256:abc123..."
    },
    "remove_custom_fields": [FIELD_ID_TO_REMOVE]
  }
}
```

## API Versioning

**Current Version:** 9 (as of provided documentation)
**Header:** `Accept: application/json; version=9`

**Version Detection:**
1. Make authenticated request to any endpoint
2. Check response headers:
   ```
   X-Api-Version: 9
   X-Version: 1.x.x
   ```

**Version Policy:** Older versions supported for ≥1 year after new version release

### Critical Version Changes for Pipeline
- **Version 7:** Select custom field format changed to objects with `id`/`label`
- **Version 8:** Document notes user field returns user object vs ID
- **Version 9:** Document `created` field is date (not datetime)

## Pipeline Integration Points

### Document Polling Strategy
1. **Query with selectors:**
   ```
   GET /api/documents/?tags=INBOX,OCR_QUEUE&tags_exclude=PROCESSED
   ```

2. **Custom field filtering for idempotency:**
   ```
   GET /api/documents/?custom_field_query=["pipeline_version", "exact", null]
   ```

3. **Filename pattern matching:**
   Use search query with filename patterns

### Processing Workflow
1. **Download document content** via document detail endpoint
2. **Update processing status** via bulk edit (add "PROCESSING" tag)
3. **Store pipeline metadata** in custom fields (version hash, document hash)
4. **Update final metadata** (title, correspondent, tags, custom fields)
5. **Mark as processed** (remove queue tags, add "PROCESSED" tag)

### Error Handling
- **Task monitoring** for upload/consumption failures
- **Bulk operation status** tracking
- **Version compatibility** checks on startup
- **Token refresh** handling for long-running processes

## Security Considerations
- **Token rotation** capability via profile UI
- **Permissions-based access** to documents and metadata
- **HTTPS-only** communication in production
- **API rate limiting** awareness (implement backoff)

## Implementation Priorities for Pipeline
1. **High Priority:** Token authentication, document search/filtering, bulk tag operations
2. **Medium Priority:** Custom field operations, task monitoring, document upload
3. **Lower Priority:** Advanced search, permissions management, API versioning detection
