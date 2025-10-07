# 🐛 Debug: Token Cache sempre gerando novo

## 🔍 **Verificações necessárias:**

### **1. Verificar se o cache está habilitado**
```bash
# Nos logs da aplicação, procure por:
# ❌ Cache de tokens desabilitado
# ✅ Cache de tokens habilitado
```

### **2. Verificar se Redis está funcionando**
```bash
# Conectar ao Redis
docker exec -it redis-container redis-cli
AUTH Teste@123

# Verificar se está funcionando
PING

# Ver todas as chaves
KEYS *

# Ver especificamente chaves de token de cliente
KEYS client_token:*
```

### **3. Verificar logs da aplicação**
Execute duas requisições seguidas e observe os logs:

**Primeira requisição:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"clientId":"client1","clientSecret":"secret1"}'
```

**Segunda requisição (imediatamente após):**
```bash
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"clientId":"client1","clientSecret":"secret1"}'
```

### **4. Logs esperados:**

**Primeira requisição (gerar novo):**
```
DEBUG - Verificando token existente para cliente: client1
DEBUG - 🔍 Procurando token para cliente: client1 (chave: client_token:client1)
DEBUG - ❌ Nenhum token encontrado no cache para cliente: client1
INFO  - 🆕 Novo token gerado para cliente: client1
DEBUG - 💾 Token associado ao cliente: client1 por X minutos
```

**Segunda requisição (reutilizar):**
```
DEBUG - Verificando token existente para cliente: client1
DEBUG - 🔍 Procurando token para cliente: client1 (chave: client_token:client1)
DEBUG - 📦 Token encontrado no cache para cliente: client1
DEBUG - ✅ Token não está na blacklist
DEBUG - ✅ Token válido encontrado no cache de validação para cliente: client1
INFO  - ✅ Token existente reutilizado para cliente: client1
```

### **5. Possíveis problemas:**

**A. Cache desabilitado:**
- Logs mostram: "❌ Cache de tokens desabilitado"
- **Solução**: Verificar `cache.token.enabled=true` no application-dev.properties

**B. Redis não conectando:**
- Logs mostram: "💥 Erro ao verificar token existente"
- **Solução**: Verificar se Redis está rodando e autenticação correta

**C. Token não sendo armazenado:**
- Primeira requisição não mostra: "💾 Token associado ao cliente"
- **Solução**: Verificar se `cacheClientToken` está sendo chamado

**D. Cache de validação não funciona:**
- Logs mostram: "❌ Token não encontrado no cache de validação"
- **Solução**: Verificar se `cacheTokenValidation` está sendo chamado

**E. TTL muito baixo:**
- Token expira antes da segunda requisição
- **Solução**: Aumentar `cache.token.ttl-minutes` temporariamente

### **6. Teste manual no Redis:**

```bash
# Ver se token foi armazenado
GET client_token:client1

# Ver TTL da chave
TTL client_token:client1

# Ver cache de validação
KEYS token_cache:*

# Ver se há problemas de conexão
INFO clients
```

### **7. Debug adicional:**

Adicione este endpoint temporário para debug:

```java
@GetMapping("/debug/cache/{clientId}")
public ResponseEntity<Map<String, Object>> debugCache(@PathVariable String clientId) {
    Map<String, Object> debug = new HashMap<>();

    debug.put("cacheEnabled", tokenCacheService.isCacheEnabled());
    debug.put("hasToken", tokenCacheService.getValidTokenForClient(clientId).isPresent());
    debug.put("redisConnection", "teste conexão Redis");

    return ResponseEntity.ok(debug);
}
```

## 🎯 **Próximos Passos:**

1. Execute as duas requisições e copie os logs aqui
2. Verifique o Redis com os comandos acima
3. Identifique qual das situações A-E está acontecendo
4. Aplique a solução correspondente