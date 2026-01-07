# WebSocket Spring Boot Application - 1-to-1 Chat

## 📌 Overview
This is a Spring Boot application that demonstrates **real-time 1-to-1 private messaging** using WebSockets. Messages are sent directly between users, not broadcast to everyone.

---

## 🎯 Key Features

✅ **Private Messaging**: Send messages to specific users only  
✅ **User Identification**: Each user has a unique userId  
✅ **Online/Offline Detection**: Server tracks connected users  
✅ **Real-time Communication**: Instant message delivery  
✅ **No Database Required**: In-memory session management  

---

## 🏗️ Project Structure

### **Dependencies (pom.xml)**
The key dependency for WebSocket support is:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

This dependency includes:
- `org.springframework.web.socket.*` - WebSocket core classes
- `org.springframework.web.socket.config.annotation.*` - Configuration annotations
- `org.springframework.web.socket.handler.*` - Handler classes

---

## 📁 File Explanations

### **1. WebSOcketsApplication.java**
```java
@SpringBootApplication
public class WebSOcketsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebSOcketsApplication.class, args);
    }
}
```
- **Purpose**: Main entry point of the application
- **@SpringBootApplication**: Composite annotation that enables:
  - Component scanning
  - Auto-configuration
  - Configuration properties
- **Function**: Bootstraps the Spring Boot application and starts embedded Tomcat server

---

### **2. WebSocketConfig.java**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new SimpleWebSocketHandler(), "/ws")
                .setAllowedOrigins("*");
    }
}
```
- **Purpose**: Configures WebSocket endpoints and handlers
- **@Configuration**: Marks as a Spring configuration class
- **@EnableWebSocket**: Enables WebSocket support in Spring
- **WebSocketConfigurer**: Interface that requires implementing handler registration
- **registerWebSocketHandlers()**: 
  - Maps the WebSocket handler to the `/ws` endpoint
  - `setAllowedOrigins("*")`: Allows connections from any origin (for development)
  - In production, specify allowed origins: `.setAllowedOrigins("https://yourdomain.com")`

---

### **3. SimpleWebSocketHandler.java**
```java
public class SimpleWebSocketHandler extends TextWebSocketHandler {
    // userId -> WebSocketSession mapping
    private static final Map<String, WebSocketSession> users = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        users.put(userId, session);
        System.out.println("User connected: " + userId);
        System.out.println("Online users: " + users.keySet());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String senderId = getUserId(session);
        String payload = message.getPayload(); // Format: receiverId:message
        String[] parts = payload.split(":", 2);
        
        if (parts.length != 2) {
            session.sendMessage(new TextMessage("Invalid format. Use receiverId:message"));
            return;
        }
        
        String receiverId = parts[0];
        String content = parts[1];
        WebSocketSession receiverSession = users.get(receiverId);
        
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage("From " + senderId + ": " + content));
        } else {
            session.sendMessage(new TextMessage("User " + receiverId + " is offline"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        users.remove(userId);
        System.out.println("User disconnected: " + userId);
    }
    
    private String getUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return "anonymous";
        
        for (String param : uri.getQuery().split("&")) {
            if (param.startsWith("userId=")) {
                return param.substring("userId=".length());
            }
        }
        return "anonymous";
    }
}
```
- **Purpose**: Handles WebSocket connections and routes messages to specific users
- **TextWebSocketHandler**: Base class for text-based WebSocket handlers
- **users Map**: Stores `userId -> WebSocketSession` mapping for routing messages
- **Message Format**: Expects `receiverId:message` (e.g., `alice:Hello Alice`)
- **afterConnectionEstablished()**: 
  - Extracts userId from query parameter (`?userId=alice`)
  - Stores session in users map
  - Logs connection and lists online users
- **handleTextMessage()**: 
  - Parses message format: `receiverId:content`
  - Validates format (must have colon separator)
  - Finds receiver's WebSocket session
  - Sends message **only** to the intended recipient
  - Notifies sender if recipient is offline
- **afterConnectionClosed()**: 
  - Removes user from users map
  - Logs disconnection
- **getUserId()**: 
  - Extracts userId from WebSocket URL query parameters
  - Returns "anonymous" if no userId provided

---

### **4. HomeController.java** (Optional)
```java
@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }
}
```
- **Purpose**: Redirects root URL to the WebSocket test client page
- **@Controller**: Marks as a Spring MVC controller
- **@GetMapping("/)**: Maps HTTP GET requests to the root path

---

### **5. chat.html**
Located in `src/main/resources/static/chat.html`
- **Purpose**: Interactive 1-to-1 WebSocket chat client
- **Features**:
  - User ID input field
  - Recipient selector
  - Connect/Disconnect buttons
  - Message input field
  - Real-time message display with sent/received/system styling
  - Connection status indicator
  - Online users indicator (check server console)
- **How it works**:
  - User enters their userId (e.g., "alice")
  - Creates WebSocket connection to `ws://localhost:8080/ws?userId=alice`
  - User enters recipient userId (e.g., "bob")
  - Sends messages in format: `bob:Hello Bob`
  - Displays sent messages on right (blue), received on left (gray)
  - Shows system messages (offline, errors) in center (yellow)
  - **No CSP issues** because it's served from the same origin

---

## 🚀 How to Run

### **1. Start the Application**
```bash
cd /home/imanitim/CODE/WebSockets/JavaSOckets/WebSOckets
mvn spring-boot:run
```

### **2. Access the Web Client**
Open your browser and go to:
```
http://localhost:8080
```
Or directly: `http://localhost:8080/chat.html`

### **3. Test 1-to-1 Chat (Two Users)**

#### **Step 1: Open First Tab (Alice)**
1. Open `http://localhost:8080` in browser
2. You'll see a random userId like "user123" - change it to `alice`
3. Click **Connect**
4. You should see: "You are now connected as 'alice'"

#### **Step 2: Open Second Tab (Bob)**
1. Open `http://localhost:8080` in a **new tab/window**
2. Change userId to `bob`
3. Click **Connect**
4. You should see: "You are now connected as 'bob'"

#### **Step 3: Send Messages**

**From Alice to Bob:**
- In Alice's tab, type in "To:" field: `bob`
- Type message: `Hello Bob!`
- Click **Send**

**Bob receives:**
- Bob's tab shows: `From alice: Hello Bob!`

**From Bob to Alice:**
- In Bob's tab, type in "To:" field: `alice`
- Type message: `Hi Alice!`
- Click **Send**

**Alice receives:**
- Alice's tab shows: `From bob: Hi Alice!`

### **4. Check Server Logs**
In your terminal, you should see:
```
User connected: alice
Online users: [alice]
User connected: bob
Online users: [alice, bob]
User disconnected: alice
Online users: [bob]
```

---

## 🔧 How WebSocket 1-to-1 Chat Works

### **Connection Flow**
```
Alice (Browser)              Server (Spring Boot)           Bob (Browser)
      |                             |                             |
      |---(1) Connect ?userId=alice->|                             |
      |                             | [Store alice -> session]    |
      |<--(2) Connection OK---------|                             |
      |                             |<---(3) Connect ?userId=bob---|
      |                             | [Store bob -> session]       |
      |                             |----(4) Connection OK-------->|
      |                             |                             |
      |---(5) bob:Hello Bob-------->|                             |
      |                             | [Find bob's session]         |
      |                             |----(6) From alice: Hello Bob->|
      |                             |                             |
      |                             |<---(7) alice:Hi Alice!-------|
      |                             | [Find alice's session]       |
      |<--(8) From bob: Hi Alice!---|                             |
      |                             |                             |
```

### **Message Routing Process**
1. **User connects** with `?userId=alice` → Server stores `alice -> session` in Map
2. **Alice sends** `bob:Hello Bob` → Server parses: recipient=`bob`, message=`Hello Bob`
3. **Server looks up** bob's session in the Map
4. **Server sends** to bob's session only: `From alice: Hello Bob`
5. **No broadcast** - only Bob receives the message

### **Key Architecture Points**
- **ConcurrentHashMap**: Thread-safe storage of `userId -> WebSocketSession`
- **Query Parameters**: Users identified via `?userId=xxx` in WebSocket URL
- **Message Format**: `receiverId:message` (simple protocol)
- **Direct Routing**: Messages sent to specific session, not broadcast
- **Online Detection**: If recipient not in Map or session closed, sender gets "offline" notification

---

## 🐛 Troubleshooting CSP Error

### **Problem**
```
Violates the following Content Security Policy directive: "connect-src chrome://resources chrome://theme 'self'"
```

### **Cause**
- This error occurs when trying to connect to WebSocket from:
  - Browser DevTools console
  - Browser extensions
  - External contexts with strict CSP

### **Solution ✅**
Use the provided `index.html` file which:
- Is served from the same origin (`http://localhost:8080`)
- Has no CSP restrictions
- Provides a proper UI for testing

---

## 📝 Additional Configuration

### **Change Server Port**
Edit `src/main/resources/application.properties`:
```properties
server.port=9090
```

### **Add STOMP Support** (Advanced)
For more complex messaging patterns, consider STOMP protocol:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```
(Already included - STOMP is part of spring-boot-starter-websocket)

---

## 🎯 Key Points

1. **Dependency**: `spring-boot-starter-websocket` provides all WebSocket classes
2. **Endpoint**: WebSocket is available at `ws://localhost:8080/ws`
3. **Handler**: `SimpleWebSocketHandler` processes all messages
4. **CORS**: Currently allows all origins (`*`) - restrict in production
5. **Testing**: Use the provided HTML client to avoid CSP issues

---

## 🔐 Security Considerations

### **For Production**
1. **Restrict Origins**:
   ```java
   .setAllowedOrigins("https://yourdomain.com", "https://www.yourdomain.com")
   ```

2. **Add Authentication**:
   ```java
   public class SimpleWebSocketHandler extends TextWebSocketHandler {
       @Override
       public void afterConnectionEstablished(WebSocketSession session) {
           // Verify authentication token
           String token = session.getHandshakeHeaders().getFirst("Authorization");
           // Validate token...
       }
   }
   ```

3. **Use WSS (Secure WebSocket)**:
   - Deploy with HTTPS
   - WebSocket will automatically upgrade to `wss://`

---

## 📚 References
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [WebSocket API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)

