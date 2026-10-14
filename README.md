# distributed-cache

> Serviço de consulta de saldo e limite de contas com cache distribuído Redis (padrão Cache-Aside).

---

### Sobre
O **distributed-cache** expõe uma API para consultar saldo e limite de contas. Para reduzir a latência e a carga no banco de dados, as leituras passam por um cache distribuído em Redis usando o padrão **Cache-Aside**:

1. A aplicação consulta o Redis.
2. **Hit:** retorna direto do cache.
3. **Miss:** busca na fonte da verdade, grava no Redis com TTL e retorna.

### Stack
- Java 21
- Spring Boot 4.0.8
- Redis 7.4
- Maven
- Docker Compose

### Pré-requisitos
- JDK 21
- Docker e Docker Compose

### Como executar
```bash
# 1. Subir o Redis
docker compose up -d

# 2. Rodar a aplicação
./mvnw spring-boot:run

# 3. Verificar a saúde
curl http://localhost:8080/actuator/health
```

### Testes
```bash
./mvnw verify
```

### Arquitetura
Arquitetura hexagonal (ports & adapters), com o domínio isolado de frameworks:

```
domain → application → adapter (in/out) → infrastructure
```

### Roadmap
- [x] Fase 0: scaffold, Redis local, CI
- [x] Fase 1: domínio
- [x] Fase 2: casos de uso e ports
- [ ] Fase 3: persistência (PostgreSQL + Flyway)
- [ ] Fase 4: API REST
- [ ] Fase 5: Cache-Aside com Redis
- [ ] Fase 6: TTL, invalidação e resiliência
- [ ] Fase 7: observabilidade
- [ ] Fase 8: testes de integração (Testcontainers)
- [ ] Fase 9: Docker e documentação final
