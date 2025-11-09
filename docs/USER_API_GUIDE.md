# User API Guide

## Overview

The Country Reference Service provides a RESTful API for managing country data based on ISO 3166 standards. This guide provides comprehensive documentation for all API endpoints, including request/response examples, authentication, error handling, and versioning behavior.

**Base URL:**
- Production: `https://api.example.com/v1`
- Local Development: `http://localhost:8080/api/v1`

**API Version:** v1

---

## Table of Contents

- [User API Guide](#user-api-guide)
  - [Overview](#overview)
  - [Table of Contents](#table-of-contents)
  - [Quick Start](#quick-start)
    - [1. Get Your API Key](#1-get-your-api-key)
    - [2. Make Your First Request](#2-make-your-first-request)
    - [3. Explore the API](#3-explore-the-api)
  - [Authentication](#authentication)
    - [Header](#header)
    - [Example](#example)
    - [Error Response](#error-response)
  - [Endpoints](#endpoints)
    - [List All Countries](#list-all-countries)
    - [Create Country](#create-country)
    - [Get Country by Alpha-2 Code](#get-country-by-alpha-2-code)
    - [Get Country by Alpha-3 Code](#get-country-by-alpha-3-code)
    - [Get Country by Numeric Code](#get-country-by-numeric-code)
    - [Update Country](#update-country)
    - [Delete Country](#delete-country)
    - [Get Country History](#get-country-history)
  - [Data Models](#data-models)
    - [Country](#country)
    - [CountryInput](#countryinput)
  - [Error Handling](#error-handling)
    - [Error Codes](#error-codes)
    - [Example Error Responses](#example-error-responses)
  - [Versioning \& History](#versioning--history)
    - [How Versioning Works](#how-versioning-works)
    - [Accessing History](#accessing-history)
    - [Example: Versioning Flow](#example-versioning-flow)
  - [Examples](#examples)
    - [Complete Workflow Example](#complete-workflow-example)
      - [1. Create a Country](#1-create-a-country)
      - [2. Retrieve by Alpha-2 Code](#2-retrieve-by-alpha-2-code)
      - [3. Update the Country](#3-update-the-country)
      - [4. View History](#4-view-history)
      - [5. Retrieve by Alpha-3 Code](#5-retrieve-by-alpha-3-code)
      - [6. Retrieve by Numeric Code](#6-retrieve-by-numeric-code)
      - [7. Delete the Country](#7-delete-the-country)
      - [8. Verify Deletion](#8-verify-deletion)
  - [Additional Resources](#additional-resources)
  - [Support](#support)

---

## Quick Start

### 1. Get Your API Key

Contact your administrator to obtain an API key. The API key must be included in all requests via the `X-API-KEY` header.

### 2. Make Your First Request

```bash
curl -X GET "http://localhost:8080/api/v1/countries?limit=5" \
  -H "X-API-KEY: your-api-key-here"
```

### 3. Explore the API

Visit the Swagger UI at `http://localhost:8080/swagger-ui.html` (when running locally) to interactively explore all endpoints.

---

## Authentication

All API endpoints require authentication via an API key.

### Header

Include the API key in the `X-API-KEY` header:

```
X-API-KEY: your-api-key-here
```

### Example

```bash
curl -X GET "http://localhost:8080/api/v1/countries" \
  -H "X-API-KEY: your-api-key-here"
```

### Error Response

If the API key is missing or invalid, you'll receive a `401 Unauthorized` response:

```json
{
  "timestamp": "2025-01-20T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid API key",
  "path": "/api/v1/countries"
}
```

---

## Endpoints

### List All Countries

Retrieves a paginated list of the latest version of all country records.

**Endpoint:** `GET /countries`

**Query Parameters:**
- `limit` (integer, optional): Maximum number of countries to return. Default: 20, Min: 1, Max: 100
- `offset` (integer, optional): Number of countries to skip. Default: 0, Min: 0

**Request Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/countries?limit=10&offset=0" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK`
```json
[
  {
    "name": "United Kingdom",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826",
    "createDate": "2025-01-20T10:00:00Z",
    "expiryDate": null,
    "isDeleted": false
  },
  {
    "name": "United States of America",
    "alpha2Code": "US",
    "alpha3Code": "USA",
    "numericCode": "840",
    "createDate": "2025-01-20T10:00:00Z",
    "expiryDate": null,
    "isDeleted": false
  }
]
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid API key
- `500 Internal Server Error`: Server error

---

### Create Country

Adds a new country record to the system. The combination of alpha2, alpha3, and numeric codes must be unique.

**Endpoint:** `POST /countries`

**Request Body:**
```json
{
  "name": "United Kingdom",
  "alpha2Code": "GB",
  "alpha3Code": "GBR",
  "numericCode": "826"
}
```

**Request Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/countries" \
  -H "X-API-KEY: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "United Kingdom",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826"
  }'
```

**Response:** `201 Created`
```json
{
  "name": "United Kingdom",
  "alpha2Code": "GB",
  "alpha3Code": "GBR",
  "numericCode": "826",
  "createDate": "2025-01-20T10:00:00Z",
  "expiryDate": null,
  "isDeleted": false
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request body or validation error
- `401 Unauthorized`: Missing or invalid API key
- `409 Conflict`: Country with the given code(s) already exists
- `500 Internal Server Error`: Server error

**Validation Rules:**
- `name`: Required, string
- `alpha2Code`: Required, exactly 2 uppercase letters (e.g., "GB")
- `alpha3Code`: Required, exactly 3 uppercase letters (e.g., "GBR")
- `numericCode`: Required, exactly 3 digits (e.g., "826")

---

### Get Country by Alpha-2 Code

Retrieves the latest version of a country by its ISO 3166-1 alpha-2 code.

**Endpoint:** `GET /countries/code/{alpha2Code}`

**Path Parameters:**
- `alpha2Code` (string, required): ISO 3166-1 alpha-2 code (e.g., "GB", "US")

**Request Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK`
```json
{
  "name": "United Kingdom",
  "alpha2Code": "GB",
  "alpha3Code": "GBR",
  "numericCode": "826",
  "createDate": "2025-01-20T10:00:00Z",
  "expiryDate": null,
  "isDeleted": false
}
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid API key
- `404 Not Found`: Country not found
- `500 Internal Server Error`: Server error

---

### Get Country by Alpha-3 Code

Retrieves the latest version of a country by its ISO 3166-1 alpha-3 code.

**Endpoint:** `GET /countries/code3/{alpha3Code}`

**Path Parameters:**
- `alpha3Code` (string, required): ISO 3166-1 alpha-3 code (e.g., "GBR", "USA")

**Request Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/countries/code3/GBR" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK`
```json
{
  "name": "United Kingdom",
  "alpha2Code": "GB",
  "alpha3Code": "GBR",
  "numericCode": "826",
  "createDate": "2025-01-20T10:00:00Z",
  "expiryDate": null,
  "isDeleted": false
}
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid API key
- `404 Not Found`: Country not found
- `500 Internal Server Error`: Server error

---

### Get Country by Numeric Code

Retrieves the latest version of a country by its ISO 3166-1 numeric code.

**Endpoint:** `GET /countries/number/{numericCode}`

**Path Parameters:**
- `numericCode` (string, required): ISO 3166-1 numeric code (e.g., "826", "840")

**Request Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/countries/number/826" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK`
```json
{
  "name": "United Kingdom",
  "alpha2Code": "GB",
  "alpha3Code": "GBR",
  "numericCode": "826",
  "createDate": "2025-01-20T10:00:00Z",
  "expiryDate": null,
  "isDeleted": false
}
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid API key
- `404 Not Found`: Country not found
- `500 Internal Server Error`: Server error

---

### Update Country

Modifies an existing country record using its primary `alpha2Code` identifier. This action creates a new version of the data, preserving the previous version in history.

**Endpoint:** `PUT /countries/code/{alpha2Code}`

**Path Parameters:**
- `alpha2Code` (string, required): ISO 3166-1 alpha-2 code (e.g., "GB")

**Request Body:**
```json
{
  "name": "United Kingdom of Great Britain and Northern Ireland",
  "alpha2Code": "GB",
  "alpha3Code": "GBR",
  "numericCode": "826"
}
```

**Request Example:**
```bash
curl -X PUT "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "United Kingdom of Great Britain and Northern Ireland",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826"
  }'
```

**Response:** `200 OK`
```json
{
  "name": "United Kingdom of Great Britain and Northern Ireland",
  "alpha2Code": "GB",
  "alpha3Code": "GBR",
  "numericCode": "826",
  "createDate": "2025-01-20T11:00:00Z",
  "expiryDate": null,
  "isDeleted": false
}
```

**Note:** The `createDate` reflects when this new version was created. The previous version will have its `expiryDate` set to this timestamp.

**Error Responses:**
- `400 Bad Request`: Invalid request body or validation error
- `401 Unauthorized`: Missing or invalid API key
- `404 Not Found`: Country not found
- `500 Internal Server Error`: Server error

---

### Delete Country

Marks a country record as deleted using its primary `alpha2Code` identifier. This is a logical deletion; the record and its history are preserved.

**Endpoint:** `DELETE /countries/code/{alpha2Code}`

**Path Parameters:**
- `alpha2Code` (string, required): ISO 3166-1 alpha-2 code (e.g., "GB")

**Request Example:**
```bash
curl -X DELETE "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `204 No Content`

**Note:** After deletion, the country will have `isDeleted: true`. It will not appear in list operations, but can still be retrieved by code and its history remains accessible.

**Error Responses:**
- `401 Unauthorized`: Missing or invalid API key
- `404 Not Found`: Country not found
- `500 Internal Server Error`: Server error

---

### Get Country History

Retrieves the complete, ordered version history for a specific country.

**Endpoint:** `GET /countries/code/{alpha2Code}/history`

**Path Parameters:**
- `alpha2Code` (string, required): ISO 3166-1 alpha-2 code (e.g., "GB")

**Request Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/countries/code/GB/history" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK`
```json
[
  {
    "name": "United Kingdom of Great Britain and Northern Ireland",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826",
    "createDate": "2025-01-20T11:00:00Z",
    "expiryDate": null,
    "isDeleted": false
  },
  {
    "name": "United Kingdom",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826",
    "createDate": "2025-01-20T10:00:00Z",
    "expiryDate": "2025-01-20T11:00:00Z",
    "isDeleted": false
  }
]
```

**Note:** The history is ordered from newest to oldest. The current version has `expiryDate: null`, while previous versions have their `expiryDate` set to the `createDate` of the next version.

**Error Responses:**
- `401 Unauthorized`: Missing or invalid API key
- `404 Not Found`: Country not found
- `500 Internal Server Error`: Server error

---

## Data Models

### Country

Represents a country record, including system-generated fields.

```json
{
  "name": "string",
  "alpha2Code": "string (2 uppercase letters)",
  "alpha3Code": "string (3 uppercase letters)",
  "numericCode": "string (3 digits)",
  "createDate": "ISO 8601 datetime (UTC)",
  "expiryDate": "ISO 8601 datetime (UTC) or null",
  "isDeleted": "boolean"
}
```

**Fields:**
- `name` (string, required): Official name of the country
- `alpha2Code` (string, required): ISO 3166-1 alpha-2 code (e.g., "GB")
- `alpha3Code` (string, required): ISO 3166-1 alpha-3 code (e.g., "GBR")
- `numericCode` (string, required): ISO 3166-1 numeric code (e.g., "826")
- `createDate` (datetime, read-only): UTC timestamp when this version was created
- `expiryDate` (datetime, read-only, nullable): UTC timestamp when this version became obsolete. `null` for the current active version
- `isDeleted` (boolean, read-only): Flag indicating if the country is logically deleted

### CountryInput

Used for create and update operations. Same as Country but without system-generated fields.

```json
{
  "name": "string",
  "alpha2Code": "string (2 uppercase letters)",
  "alpha3Code": "string (3 uppercase letters)",
  "numericCode": "string (3 digits)"
}
```

---

## Error Handling

All error responses follow a consistent format:

```json
{
  "timestamp": "ISO 8601 datetime (UTC)",
  "status": "integer (HTTP status code)",
  "error": "string (error type)",
  "message": "string (human-readable description)",
  "path": "string (request path)"
}
```

### Error Codes

| Status Code | Error Type | Description |
|------------|-----------|-------------|
| 400 | Bad Request | Invalid request body or validation error |
| 401 | Unauthorized | Missing or invalid API key |
| 404 | Not Found | Requested resource does not exist |
| 409 | Conflict | Resource already exists (e.g., duplicate country codes) |
| 500 | Internal Server Error | Unexpected server error |

### Example Error Responses

**400 Bad Request:**
```json
{
  "timestamp": "2025-01-20T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid alpha2Code format. Must be exactly 2 uppercase letters.",
  "path": "/api/v1/countries"
}
```

**404 Not Found:**
```json
{
  "timestamp": "2025-01-20T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Country with alpha2Code 'XX' not found.",
  "path": "/api/v1/countries/code/XX"
}
```

**409 Conflict:**
```json
{
  "timestamp": "2025-01-20T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "A country with the given code(s) already exists.",
  "path": "/api/v1/countries"
}
```

---

## Versioning & History

The Country Reference Service implements a write-once versioning pattern:

### How Versioning Works

1. **Create:** When you create a country, it becomes version 1 with `expiryDate: null`
2. **Update:** When you update a country:
   - The current version's `expiryDate` is set to the new version's `createDate`
   - A new version is created with `expiryDate: null`
   - The previous version is preserved in history
3. **Delete:** When you delete a country:
   - The current version is marked with `isDeleted: true`
   - All history is preserved
   - The country no longer appears in list operations

### Accessing History

Use the history endpoint to retrieve all versions of a country:

```bash
GET /countries/code/{alpha2Code}/history
```

The response is ordered from newest to oldest. Each version includes:
- The data as it existed at that point in time
- `createDate`: When this version was created
- `expiryDate`: When this version became obsolete (or `null` for the current version)
- `isDeleted`: Whether the country was deleted at this version

### Example: Versioning Flow

1. **Create country:**
   ```json
   {
     "name": "United Kingdom",
     "alpha2Code": "GB",
     "createDate": "2025-01-20T10:00:00Z",
     "expiryDate": null,
     "isDeleted": false
   }
   ```

2. **Update country:**
   ```json
   {
     "name": "United Kingdom of Great Britain and Northern Ireland",
     "alpha2Code": "GB",
     "createDate": "2025-01-20T11:00:00Z",
     "expiryDate": null,
     "isDeleted": false
   }
   ```

3. **History now contains:**
   - Version 2 (current): `createDate: 2025-01-20T11:00:00Z`, `expiryDate: null`
   - Version 1 (previous): `createDate: 2025-01-20T10:00:00Z`, `expiryDate: 2025-01-20T11:00:00Z`

---

## Examples

### Complete Workflow Example

This example demonstrates creating, updating, retrieving, and viewing history for a country.

#### 1. Create a Country

```bash
curl -X POST "http://localhost:8080/api/v1/countries" \
  -H "X-API-KEY: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "United Kingdom",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826"
  }'
```

**Response:** `201 Created`

#### 2. Retrieve by Alpha-2 Code

```bash
curl -X GET "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK`

#### 3. Update the Country

```bash
curl -X PUT "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "United Kingdom of Great Britain and Northern Ireland",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826"
  }'
```

**Response:** `200 OK`

#### 4. View History

```bash
curl -X GET "http://localhost:8080/api/v1/countries/code/GB/history" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK` with both versions

#### 5. Retrieve by Alpha-3 Code

```bash
curl -X GET "http://localhost:8080/api/v1/countries/code3/GBR" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK` with latest version

#### 6. Retrieve by Numeric Code

```bash
curl -X GET "http://localhost:8080/api/v1/countries/number/826" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK` with latest version

#### 7. Delete the Country

```bash
curl -X DELETE "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `204 No Content`

#### 8. Verify Deletion

```bash
curl -X GET "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here"
```

**Response:** `200 OK` with `isDeleted: true`

---

## Additional Resources

- **OpenAPI Specification:** See `openapi.yml` in the repository
- **Swagger UI:** Available at `http://localhost:8080/swagger-ui.html` (when running locally)
- **Sample Data:** See `countries_iso3166b.csv` for example country data

---

## Support

For issues, questions, or feature requests, please contact your administrator or refer to the project repository.

