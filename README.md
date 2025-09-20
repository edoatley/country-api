# Country Reference Service

This document provides the technical specification for the Country Reference Service, a microservice designed to provide and manage country data. It is built following modern software architecture principles to ensure scalability, maintainability, and testability.

## 1. Objectives

The primary objective of this project is to create a robust, scalable, and maintainable Country Reference Service API. This service will act as a centralized source of truth for country information, supporting a full change history for all data entities.  
Key goals include:

* **Architectural Excellence:** Implement a clean, loosely coupled system using the Hexagonal Architecture (Ports and Adapters) pattern. This ensures the core business logic is isolated from external technologies and frameworks, facilitating independent evolution and testing.1  
* **Technology Independence:** Decouple the application core from specific infrastructure choices, such as the database (DynamoDB) and the request handlers (REST, AWS Lambda). This allows for future flexibility, such as migrating to a different database without impacting the core logic.  
* **Comprehensive Functionality:** Provide a complete set of CRUD (Create, Read, Update, Delete) operations for country data, alongside queries to retrieve all countries and search by a specific country code.  
* **Data Integrity and Auditing:** Implement a versioned data model within DynamoDB to maintain a complete, auditable change history for every country record.
* **Developer Efficiency:** Establish a modern, local-first development and testing ecosystem using LocalStack and Testcontainers, enabling developers to build and validate the application without direct reliance on live AWS resources.

## 2. Requirements

This section details the mandatory architectural, functional, and technical requirements for the service.

### 2.1. Architecture: Hexagonal (Ports & Adapters)

The service must adhere to the principles of Hexagonal Architecture, also known as the Ports and Adapters pattern.

* **Core Principle:** The central application core, containing all business logic and domain models, must have no dependencies on external frameworks or infrastructure code. Dependencies must always point inwards, from the adapters to the core.
* **Project Structure:** The codebase will be physically separated into a multi-module project to enforce these architectural boundaries.
  * country-service-domain: Contains the Country domain model (a plain Java record). It has zero dependencies on other project modules.  
  * country-service-application: Contains the interfaces for interacting with the core (ports) and the use case implementations (services). It depends only on the domain module.  
  * country-service-adapters: Contains all infrastructure-specific implementations (e.g., REST controllers, Lambda handlers, DynamoDB repositories). It depends on the application module to implement its ports.  
  * country-service-bootstrap: The application entry point responsible for dependency injection and wiring all components together at startup.  
* **Architectural Testing:** Architectural integrity must be enforced via automated tests using the ArchUnit library with JUnit5.13 These tests will codify the dependency rules, preventing architectural drift. For example, a test will ensure that no class within the  
  domain or application modules can access any class from the AWS SDK or the adapters module.15 This serves not only as a quality gate but also as living documentation of the system's design. A developer attempting to violate the architecture will receive immediate, clear feedback from a failing test, preventing the gradual erosion of the design's benefits.

### **2.2. API and Functionality**

The service will expose its functionality through two primary input adapters: a RESTful HTTP API and an AWS Lambda function. The detailed API contract is provided in the accompanying openapi.yml specification.

* **Functionality:**  
  * **Create Country:** Add a new country record.  
  * **Get Country by Code:** Retrieve the latest version of a country by its ISO code (e.g., "US").  
  * **Get Country by Number:** Retrieve the latest version of a country by its ISO number (e.g., "840").
  * **Get All Countries:** Retrieve the latest version of all country records.  
  * **Update Country:** Modify an existing country record, creating a new version.  
  * **Delete Country:** Logically delete a country record (details TBD, e.g., setting an isDeleted flag).  
  * **Get Country History:** Retrieve the complete version history for a specific country.

### **2.3. Data Model: Versioned Schema in DynamoDB**

The service will use a single-table design in Amazon DynamoDB to store all country data and its version history. The key fields to store are:

- Country Name
- Country 2 letter code
- Country 3 letter code
- Country number
- Create Date
- Expiry Date

### 2.4. Technology Stack

* **Language/Framework:** Java 21  
* **Database:** Amazon DynamoDB  
* **API:** REST (via e.g. Spring Boot, Quarkus etc), AWS Lambda with API Gateway  
* **Testing:** JUnit5, Mockito
* **Local Development:** LocalStack
* **Build Tool:** Gradle 
* **AWS Integration:** AWS SDK for Java v2

### **2.5. Local Development Environment**

Leverage LocalStack running on docker for testing. LocalStack provides a high-fidelity emulator for AWS services, allowing developers to test cloud-native applications entirely on their local machine or a shared development cluster
You can run the lambda code by setting:

```bash  
export AWS_ENDPOINT_URL='<your-localstack-openshift-route-url>'  
export AWS_ACCESS_KEY_ID='test'  
export AWS_SECRET_ACCESS_KEY='test'  
export AWS_REGION='us-east-1'
```
