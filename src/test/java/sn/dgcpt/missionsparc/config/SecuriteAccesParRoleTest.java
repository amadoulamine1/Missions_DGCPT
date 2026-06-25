package sn.dgcpt.missionsparc.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règles d'accès par rôle (§9.7) appliquées par {@link SecurityConfig}. On ne teste que la
 * couche de sécurité d'URL : aucun vrai contrôleur n'est chargé, d'où la convention d'assertion
 * — <b>403</b> = accès refusé (rôle insuffisant), <b>redirection /login</b> = non authentifié,
 * <b>404</b> = accès autorisé (la requête a franchi la sécurité sans trouver de handler).
 * CSRF étant actif, les POST autorisés portent le jeton ({@code with(csrf())}).
 */
@WebMvcTest(controllers = SecuriteAccesParRoleTest.NoopController.class)
@Import(SecurityConfig.class)
class SecuriteAccesParRoleTest {

    /** Contrôleur vide : présence d'un bean web sans mapper aucune des URL réelles testées. */
    @RestController
    static class NoopController { }

    @Autowired MockMvc mvc;

    private static RequestPostProcessor admin() { return user("admin").roles("ADMIN"); }
    private static RequestPostProcessor chef()  { return user("chef").roles("CHEF_MISSION"); }
    private static RequestPostProcessor agent() { return user("agent").roles("AGENT"); }

    // ---------- non authentifié ----------

    @Test
    void accueil_anonyme_redirige_vers_login() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void zone_admin_anonyme_redirige_vers_login() throws Exception {
        mvc.perform(get("/utilisateurs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void page_login_accessible_sans_authentification() throws Exception {
        // permitAll : la requête franchit la sécurité sans redirection (404 faute de handler
        // dans la slice web, et non une 3xx vers /login).
        mvc.perform(get("/login")).andExpect(status().isNotFound());
    }

    // ---------- rôle AGENT : consultation seulement ----------

    @Test
    void agent_refuse_sur_les_zones_administrateur() throws Exception {
        mvc.perform(get("/utilisateurs").with(agent())).andExpect(status().isForbidden());
        mvc.perform(get("/agents").with(agent())).andExpect(status().isForbidden());
        mvc.perform(get("/referentiels/logiciels").with(agent())).andExpect(status().isForbidden());
        mvc.perform(get("/postes/nouveau").with(agent())).andExpect(status().isForbidden());
        mvc.perform(get("/missions/nouvelle").with(agent())).andExpect(status().isForbidden());
        mvc.perform(post("/parc/5/affectation").with(agent()).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void agent_autorise_a_consulter_postes_et_parc() throws Exception {
        mvc.perform(get("/postes").with(agent())).andExpect(status().isNotFound());
        mvc.perform(get("/postes/5").with(agent())).andExpect(status().isNotFound());
        mvc.perform(get("/parc").with(agent())).andExpect(status().isNotFound());
    }

    // ---------- rôle CHEF_MISSION ----------

    @Test
    void chef_mission_autorise_a_gerer_ses_missions() throws Exception {
        mvc.perform(get("/missions/nouvelle").with(chef())).andExpect(status().isNotFound());
        mvc.perform(post("/missions/3/cloturer").with(chef()).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/missions/3/consolidation/integrer").with(chef()).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void chef_mission_refuse_sur_administration_et_affectation() throws Exception {
        mvc.perform(get("/utilisateurs").with(chef())).andExpect(status().isForbidden());
        mvc.perform(get("/agents").with(chef())).andExpect(status().isForbidden());
        mvc.perform(post("/parc/5/affectation").with(chef()).with(csrf())).andExpect(status().isForbidden());
    }

    // ---------- rôle ADMIN : tout autorisé ----------

    @Test
    void admin_autorise_partout() throws Exception {
        mvc.perform(get("/utilisateurs").with(admin())).andExpect(status().isNotFound());
        mvc.perform(get("/agents").with(admin())).andExpect(status().isNotFound());
        mvc.perform(get("/referentiels/logiciels").with(admin())).andExpect(status().isNotFound());
        mvc.perform(get("/postes/nouveau").with(admin())).andExpect(status().isNotFound());
        mvc.perform(get("/missions/nouvelle").with(admin())).andExpect(status().isNotFound());
        mvc.perform(post("/parc/5/affectation").with(admin()).with(csrf())).andExpect(status().isNotFound());
    }

    // ---------- protection CSRF ----------

    @Test
    void post_autorise_sans_jeton_csrf_est_refuse() throws Exception {
        mvc.perform(post("/missions/3/cloturer").with(chef()))   // pas de with(csrf())
                .andExpect(status().isForbidden());
    }
}
