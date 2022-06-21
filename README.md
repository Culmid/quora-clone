# Quora Clone

## Description
This project is a Quora clone, made to replicate the behaviour of the respective website. This is done for purely educational purposes, there is no financial incentive to do this.

Currently, the clone consists of a backend implementation in Spring Boot, with the following technologies:
- Kotlin
- Gradle
- PostgreSQL
- Redis

The endpoints provided are related to the account management needed in a website like Quora.

## Usage
**Note**: See `./gradlew tasks` for an extensive list of commands and their usages.

### Build
```bash
./gradlew build
```
Build the jar file necessary for deployment to production.

### Run
```bash
java -Dserver.port=$PORT $JAVA_OPTS -jar build/libs/quora-clone-0.0.1-SNAPSHOT.jar
```
Run the server using the jar file.

### Build + Run

```bash
./gradlew bootRun
```
Build and run the server with one command.

## Endpoints
### GET - Basic Get
**Description**

Basic GET to test if server is functional.

**Request URL**
```bash
GET /
```

**Example Response**
```json
{
  "success": true,
  "message": "GET Message"
}
```

### POST - Basic Post
**Description**

Basic POST to test if server is functional.

**Request URL**
```bash
POST /
```

**Example Response**
```json
{
  "success": true,
  "message": "POST Message",
  "data": {
    "Data": "Stuff"
  }
}
```

### POST - Register User
**Description**

POST request to register a user on the system, and hence in the PostgreSQL DB.

**Request URL**
```bash
POST /auth/register
```

**Example Request Body**
```json
{
    "firstName": "Arthur",
    "lastName": "Morgan",
    "email": "heartbreakhero@email.com",
    "password": "12345678"
}
```

**Example Response**
```json
{
  "success": true,
  "message": "User Registered",
  "data": {
    "Jwt-Token": "<JWT Token>"
  }
}
```

### POST - Login User
**Description**

POST request to login existing user, and retrieve a valid JWT Token.

**Request URL**
```bash
POST /auth/login
```

**Example Request Body**
```json
{
  "email": "heartbreakhero@email.com",
  "password": "12345678"
}
```

**Example Response**
```json
{
  "success": true,
  "message": "User Logged In",
  "data": {
    "Jwt-Token": "<JWT Token>"
  }
}
```

### POST - Change Password
**Description**

POST request to change the password of a logged-in user.

**Request URL**
```bash
POST /auth/password
```

**Required Header Field**
```json
{
  "authorization": "Bearer <Valid JWT Token>"
}
```

**Example Request Body**
```json
{
  "currentPassword": "12345678",
  "newPassword": "12345678910"
}
```

**Example Response**
```json
{
  "success": true,
  "message": "Password Updated"
}
```

### POST - Forgot Password
**Description**

POST request to reset the password of a user. It currently sends a recovery key to the user if they exist, and saves this key in redis.

**Request URL**
```bash
POST /auth/password-reset
```

**Example Request Body**
```json
{
  "email": "heartbreakhero@email.com"
}
```

**Example Response**
```json
{
  "success": true,
  "message": "5 Digit Recovery Key will be sent to: culmid@gmail.com"
}
```