# 🔧 Configuração do Security Token Service

## 📋 **Variáveis de Ambiente Obrigatórias**

### **JWT Configuration**
```bash
# Chave secreta JWT (mínimo 32 caracteres)
export JWT_SECRET="your-super-secure-jwt-secret-key-here-minimum-32-characters"

# Tempo de expiração do token em segundos (opcional, padrão: 3600)
export JWT_EXPIRATION=3600
```

### **MongoDB Configuration (Produção)**
```bash
# URI completa do MongoDB
export MONGODB_URI="mongodb://username:password@hostname:27017/database?authSource=admin"

# Nome da database (opcional, padrão: security-token-service)
export MONGODB_DATABASE="security-token-service"
```

## 🚀 **Como Executar**

### **Desenvolvimento Local**
```bash
# 1. Definir profile de desenvolvimento
export SPRING_PROFILES_ACTIVE=dev

# 2. Definir JWT secret
export JWT_SECRET="myDevSecretKey123456789012345678901234567890"

# 3. Iniciar MongoDB
docker-compose up -d mongodb

# 4. Executar aplicação
./mvnw spring-boot:run
```

### **Produção**
```bash
# 1. Definir profile de produção
export SPRING_PROFILES_ACTIVE=prod

# 2. Definir variáveis obrigatórias
export JWT_SECRET="$(openssl rand -base64 32)"
export MONGODB_URI="mongodb://user:pass@mongo-host:27017/security-token-service?authSource=admin"

# 3. Executar aplicação
java -jar security-token-service.jar
```

### **Testes**
```bash
# Profile de teste já tem configurações padrão
export SPRING_PROFILES_ACTIVE=test
./mvnw test
```

## 🔐 **Gerando JWT Secret Seguro**

```bash
# Opção 1: OpenSSL
openssl rand -base64 32

# Opção 2: Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Opção 3: Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

## 🐳 **Docker Configuration**

### **docker-compose.yml**
```yaml
services:
  security-token-service:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_SECRET=${JWT_SECRET}
      - MONGODB_URI=mongodb://admin:password@mongodb:27017/security-token-service?authSource=admin
    depends_on:
      - mongodb
```

### **.env file**
```bash
# Copie .env.example para .env e configure
cp .env.example .env
# Edite .env com seus valores
```

## 📁 **Profiles Disponíveis**

| Profile | Database | JWT Secret | Logs | Uso |
|---------|----------|------------|------|-----|
| `dev` | MongoDB local | Padrão dev | DEBUG | Desenvolvimento |
| `test` | MongoDB test | Fixo para testes | INFO | Testes automatizados |
| `prod` | Via env vars | **OBRIGATÓRIO** | WARN | Produção |

## ⚠️ **Segurança em Produção**

### **Checklist de Segurança:**
- [ ] JWT_SECRET com no mínimo 32 caracteres
- [ ] MongoDB com autenticação habilitada
- [ ] Conexões HTTPS em produção
- [ ] Logs não devem conter secrets
- [ ] Rate limiting configurado
- [ ] Backup da database configurado

### **Variáveis que NÃO devem estar no código:**
- ✅ JWT_SECRET - Agora em variável de ambiente
- ⚠️ MongoDB credentials - Partially done
- ⚠️ API keys - Se houver no futuro
- ⚠️ Certificados SSL - Se necessário

## 🔧 **Troubleshooting**

### **Erro: "JWT_SECRET environment variable is required"**
```bash
export JWT_SECRET="your-secret-here-minimum-32-characters"
```

### **Erro: "JWT_SECRET is too short"**
```bash
# Use pelo menos 32 caracteres
export JWT_SECRET="$(openssl rand -base64 32)"
```

### **Erro de conexão MongoDB**
```bash
# Verifique se MongoDB está rodando
docker-compose ps

# Verifique logs do MongoDB
docker-compose logs mongodb
```

## 📊 **Health Check**

```bash
# Verificar saúde da aplicação
curl http://localhost:8080/actuator/health

# Verificar configurações (dev only)
curl http://localhost:8080/actuator/info
```