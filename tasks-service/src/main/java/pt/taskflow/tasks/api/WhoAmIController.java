package pt.taskflow.tasks.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WhoAmIController {

    @GetMapping("/whoami")
    public Map<String, Object> whoAmI() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        Map<String, Object> info = new HashMap<>();
        info.put("service", "tasks-service");
        info.put("hostname", localHost.getHostName());
        info.put("ip", localHost.getHostAddress());
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }
}
