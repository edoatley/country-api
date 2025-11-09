# Integration Samples

This document provides sample requests for calling the Country Reference Service API using various tools and languages.

**Base URL:**
- Local: `http://localhost:8080/api/v1`
- Production: `https://api.example.com/v1`

**API Key:** Replace `your-api-key-here` with your actual API key.

---

## Table of Contents

1. [cURL](#curl)
2. [HTTPie](#httpie)
3. [Postman](#postman)
4. [JavaScript (Fetch API)](#javascript-fetch-api)
5. [Python (requests)](#python-requests)
6. [Java (HttpClient)](#java-httpclient)

---

## cURL

### List All Countries

```bash
curl -X GET "http://localhost:8080/api/v1/countries?limit=10&offset=0" \
  -H "X-API-KEY: your-api-key-here"
```

### Get Country by Alpha-2 Code

```bash
curl -X GET "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here"
```

### Get Country by Alpha-3 Code

```bash
curl -X GET "http://localhost:8080/api/v1/countries/code3/GBR" \
  -H "X-API-KEY: your-api-key-here"
```

### Get Country by Numeric Code

```bash
curl -X GET "http://localhost:8080/api/v1/countries/number/826" \
  -H "X-API-KEY: your-api-key-here"
```

### Create Country

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

### Update Country

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

### Delete Country

```bash
curl -X DELETE "http://localhost:8080/api/v1/countries/code/GB" \
  -H "X-API-KEY: your-api-key-here"
```

### Get Country History

```bash
curl -X GET "http://localhost:8080/api/v1/countries/code/GB/history" \
  -H "X-API-KEY: your-api-key-here"
```

---

## HTTPie

### List All Countries

```bash
http GET "http://localhost:8080/api/v1/countries?limit=10&offset=0" \
  X-API-KEY:your-api-key-here
```

### Get Country by Alpha-2 Code

```bash
http GET "http://localhost:8080/api/v1/countries/code/GB" \
  X-API-KEY:your-api-key-here
```

### Get Country by Alpha-3 Code

```bash
http GET "http://localhost:8080/api/v1/countries/code3/GBR" \
  X-API-KEY:your-api-key-here
```

### Get Country by Numeric Code

```bash
http GET "http://localhost:8080/api/v1/countries/number/826" \
  X-API-KEY:your-api-key-here
```

### Create Country

```bash
http POST "http://localhost:8080/api/v1/countries" \
  X-API-KEY:your-api-key-here \
  name="United Kingdom" \
  alpha2Code="GB" \
  alpha3Code="GBR" \
  numericCode="826"
```

### Update Country

```bash
http PUT "http://localhost:8080/api/v1/countries/code/GB" \
  X-API-KEY:your-api-key-here \
  name="United Kingdom of Great Britain and Northern Ireland" \
  alpha2Code="GB" \
  alpha3Code="GBR" \
  numericCode="826"
```

### Delete Country

```bash
http DELETE "http://localhost:8080/api/v1/countries/code/GB" \
  X-API-KEY:your-api-key-here
```

### Get Country History

```bash
http GET "http://localhost:8080/api/v1/countries/code/GB/history" \
  X-API-KEY:your-api-key-here
```

---

## Postman

### Import Collection

1. **Create a new collection** in Postman
2. **Add environment variables:**
   - `base_url`: `http://localhost:8080/api/v1`
   - `api_key`: `your-api-key-here`

3. **Create requests** for each endpoint:

#### List All Countries

- **Method:** `GET`
- **URL:** `{{base_url}}/countries?limit=10&offset=0`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`

#### Get Country by Alpha-2 Code

- **Method:** `GET`
- **URL:** `{{base_url}}/countries/code/GB`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`

#### Get Country by Alpha-3 Code

- **Method:** `GET`
- **URL:** `{{base_url}}/countries/code3/GBR`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`

#### Get Country by Numeric Code

- **Method:** `GET`
- **URL:** `{{base_url}}/countries/number/826`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`

#### Create Country

- **Method:** `POST`
- **URL:** `{{base_url}}/countries`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`
  - `Content-Type`: `application/json`
- **Body (raw JSON):**
  ```json
  {
    "name": "United Kingdom",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826"
  }
  ```

#### Update Country

- **Method:** `PUT`
- **URL:** `{{base_url}}/countries/code/GB`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`
  - `Content-Type`: `application/json`
- **Body (raw JSON):**
  ```json
  {
    "name": "United Kingdom of Great Britain and Northern Ireland",
    "alpha2Code": "GB",
    "alpha3Code": "GBR",
    "numericCode": "826"
  }
  ```

#### Delete Country

- **Method:** `DELETE`
- **URL:** `{{base_url}}/countries/code/GB`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`

#### Get Country History

- **Method:** `GET`
- **URL:** `{{base_url}}/countries/code/GB/history`
- **Headers:**
  - `X-API-KEY`: `{{api_key}}`

---

## JavaScript (Fetch API)

### List All Countries

```javascript
const response = await fetch('http://localhost:8080/api/v1/countries?limit=10&offset=0', {
  method: 'GET',
  headers: {
    'X-API-KEY': 'your-api-key-here'
  }
});

const countries = await response.json();
console.log(countries);
```

### Get Country by Alpha-2 Code

```javascript
const response = await fetch('http://localhost:8080/api/v1/countries/code/GB', {
  method: 'GET',
  headers: {
    'X-API-KEY': 'your-api-key-here'
  }
});

const country = await response.json();
console.log(country);
```

### Create Country

```javascript
const response = await fetch('http://localhost:8080/api/v1/countries', {
  method: 'POST',
  headers: {
    'X-API-KEY': 'your-api-key-here',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'United Kingdom',
    alpha2Code: 'GB',
    alpha3Code: 'GBR',
    numericCode: '826'
  })
});

const country = await response.json();
console.log(country);
```

### Update Country

```javascript
const response = await fetch('http://localhost:8080/api/v1/countries/code/GB', {
  method: 'PUT',
  headers: {
    'X-API-KEY': 'your-api-key-here',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'United Kingdom of Great Britain and Northern Ireland',
    alpha2Code: 'GB',
    alpha3Code: 'GBR',
    numericCode: '826'
  })
});

const country = await response.json();
console.log(country);
```

### Delete Country

```javascript
const response = await fetch('http://localhost:8080/api/v1/countries/code/GB', {
  method: 'DELETE',
  headers: {
    'X-API-KEY': 'your-api-key-here'
  }
});

if (response.status === 204) {
  console.log('Country deleted successfully');
}
```

### Get Country History

```javascript
const response = await fetch('http://localhost:8080/api/v1/countries/code/GB/history', {
  method: 'GET',
  headers: {
    'X-API-KEY': 'your-api-key-here'
  }
});

const history = await response.json();
console.log(history);
```

---

## Python (requests)

### List All Countries

```python
import requests

url = 'http://localhost:8080/api/v1/countries'
headers = {
    'X-API-KEY': 'your-api-key-here'
}
params = {
    'limit': 10,
    'offset': 0
}

response = requests.get(url, headers=headers, params=params)
countries = response.json()
print(countries)
```

### Get Country by Alpha-2 Code

```python
import requests

url = 'http://localhost:8080/api/v1/countries/code/GB'
headers = {
    'X-API-KEY': 'your-api-key-here'
}

response = requests.get(url, headers=headers)
country = response.json()
print(country)
```

### Create Country

```python
import requests

url = 'http://localhost:8080/api/v1/countries'
headers = {
    'X-API-KEY': 'your-api-key-here',
    'Content-Type': 'application/json'
}
data = {
    'name': 'United Kingdom',
    'alpha2Code': 'GB',
    'alpha3Code': 'GBR',
    'numericCode': '826'
}

response = requests.post(url, headers=headers, json=data)
country = response.json()
print(country)
```

### Update Country

```python
import requests

url = 'http://localhost:8080/api/v1/countries/code/GB'
headers = {
    'X-API-KEY': 'your-api-key-here',
    'Content-Type': 'application/json'
}
data = {
    'name': 'United Kingdom of Great Britain and Northern Ireland',
    'alpha2Code': 'GB',
    'alpha3Code': 'GBR',
    'numericCode': '826'
}

response = requests.put(url, headers=headers, json=data)
country = response.json()
print(country)
```

### Delete Country

```python
import requests

url = 'http://localhost:8080/api/v1/countries/code/GB'
headers = {
    'X-API-KEY': 'your-api-key-here'
}

response = requests.delete(url, headers=headers)
if response.status_code == 204:
    print('Country deleted successfully')
```

### Get Country History

```python
import requests

url = 'http://localhost:8080/api/v1/countries/code/GB/history'
headers = {
    'X-API-KEY': 'your-api-key-here'
}

response = requests.get(url, headers=headers)
history = response.json()
print(history)
```

---

## Java (HttpClient)

### List All Countries

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/v1/countries?limit=10&offset=0"))
    .header("X-API-KEY", "your-api-key-here")
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

### Get Country by Alpha-2 Code

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/v1/countries/code/GB"))
    .header("X-API-KEY", "your-api-key-here")
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

### Create Country

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

String json = """
    {
      "name": "United Kingdom",
      "alpha2Code": "GB",
      "alpha3Code": "GBR",
      "numericCode": "826"
    }
    """;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/v1/countries"))
    .header("X-API-KEY", "your-api-key-here")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

### Update Country

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

String json = """
    {
      "name": "United Kingdom of Great Britain and Northern Ireland",
      "alpha2Code": "GB",
      "alpha3Code": "GBR",
      "numericCode": "826"
    }
    """;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/v1/countries/code/GB"))
    .header("X-API-KEY", "your-api-key-here")
    .header("Content-Type", "application/json")
    .PUT(HttpRequest.BodyPublishers.ofString(json))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

### Delete Country

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/v1/countries/code/GB"))
    .header("X-API-KEY", "your-api-key-here")
    .DELETE()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
if (response.statusCode() == 204) {
    System.out.println("Country deleted successfully");
}
```

### Get Country History

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/v1/countries/code/GB/history"))
    .header("X-API-KEY", "your-api-key-here")
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```

---

## Additional Resources

- **User API Guide**: See `docs/USER_API_GUIDE.md` for detailed endpoint documentation
- **OpenAPI Specification**: See `openapi.yml` for the complete API specification
- **Swagger UI**: Visit `http://localhost:8080/swagger-ui.html` (when running locally) for interactive API exploration

