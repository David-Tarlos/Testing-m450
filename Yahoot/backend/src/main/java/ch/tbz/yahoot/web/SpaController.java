package ch.tbz.yahoot.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Die Seiten des Angular-Routers, damit ein Link wie /join?code=ABC234 auch direkt funktioniert. */
@Controller
class SpaController {

    @GetMapping({"/host", "/join"})
    String index() {
        return "forward:/index.html";
    }
}
