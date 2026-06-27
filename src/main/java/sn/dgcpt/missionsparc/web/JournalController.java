package sn.dgcpt.missionsparc.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sn.dgcpt.missionsparc.audit.AuditService;
import sn.dgcpt.missionsparc.audit.AuditVue;
import sn.dgcpt.missionsparc.consultation.dto.PageVue;

/** Journal d'audit (consultation, réservé à l'administrateur). */
@Controller
public class JournalController {

    private final AuditService audit;

    public JournalController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping("/journal")
    public String journal(@RequestParam(required = false) String action,
                          @RequestParam(required = false) String user,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        PageVue<AuditVue> p = audit.journal(action, user,
                PageRequest.of(Math.max(0, page), 30, Sort.by(Sort.Direction.DESC, "dateHeure")));
        model.addAttribute("page", p);
        model.addAttribute("events", p.getContenu());
        model.addAttribute("actions", audit.actions());
        model.addAttribute("fAction", action);
        model.addAttribute("fUser", user);
        return "journal";
    }
}
