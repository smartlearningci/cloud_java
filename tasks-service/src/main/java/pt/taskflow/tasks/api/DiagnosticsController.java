package pt.taskflow.tasks.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.taskflow.tasks.infra.OutboundClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controlador de "diagnóstico" para EXPLICAR e PROVAR os padrões de resiliência.
 *
 * Tem dois endpoints:
 * - /diagnostics/simulate/delay : é o "ALVO" que podemos atrasar/forçar 500 (falha)
 * - /diagnostics/outbound       : é a "CHAMADA" que usa Resilience4j + fallback
 *
 * Chamadas normais passam OK; quando pedimos atraso grande (ms=3000) ou falha (fail=true),
 * vemos timeout -> retry -> fallback e o circuit breaker a abrir/fechar.
 *
 * Estes endpoints ficam no tasks-service para não precisares de outro serviço para testar.
 * Como o baseUrl é "http://tasks-service", a chamada outbound usa Discovery + LB.
 */
@RestController
@RequestMapping("/diagnostics")
public class DiagnosticsController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsController.class);

    private final OutboundClient outbound;

    public DiagnosticsController(OutboundClient outbound) {
        this.outbound = outbound;
    }

    /**
     * ALVO CONTROLADO (no próprio serviço):
     * - Simula atraso com Thread.sleep(ms) -> didático para provocar timeout.
     * - Se fail=true, devolve 500 intencionalmente -> didático para provocar CB.
     *
     * NOTA: usar sleep em prod não é boa prática; aqui é apenas para DEMO.
     */
    @GetMapping("/simulate/delay")
    public ResponseEntity<Map<String, Object>> simulate(@RequestParam(defaultValue = "0") int ms,
                                                        @RequestParam(defaultValue = "false") boolean fail) {
        try {
            if (ms > 0) {
                Thread.sleep(ms); // simula latência
            }
        } catch (InterruptedException ignored) {}

        if (fail) {
            log.warn("simulate/delay -> returning 500 (ms={}, fail=true)", ms);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "fail",
                    "delayMs", ms
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "delayMs", ms
        ));
    }

    /**
     * CHAMADA OUTBOUND COM RESILIÊNCIA:
     * - Usa OutboundClient.callDelayed(ms, fail) protegido com timeout/retry/circuit breaker.
     * - Quando há problema, devolve o Fallback (JSON com 'source' e 'reason').
     */
    @GetMapping("/outbound")
    public Mono<String> outbound(@RequestParam(defaultValue = "0") int ms,
                                 @RequestParam(defaultValue = "false") boolean fail) {
        return outbound.callDelayed(ms, fail);
    }
}
