package sn.dgcpt.missionsparc.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sn.dgcpt.missionsparc.agent.AgentForm;
import sn.dgcpt.missionsparc.mission.PosteForm;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contraintes déclaratives (Bean Validation) sur les formulaires de saisie. On valide les
 * annotations directement via un {@link Validator}, sans Spring ni Mockito — donc sans la
 * limite JDK 25 (ByteBuddy n'instrumente pas les classes concrètes).
 */
class FormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll static void init() { factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator(); }
    @AfterAll static void close() { factory.close(); }

    private long erreursSur(Object o, String champ) {
        return validator.validate(o).stream().filter(v -> v.getPropertyPath().toString().equals(champ)).count();
    }

    @Test
    void poste_code_et_nom_obligatoires() {
        PosteForm f = new PosteForm();   // code et nom nuls
        assertThat(erreursSur(f, "code")).isEqualTo(1);
        assertThat(erreursSur(f, "nom")).isEqualTo(1);
    }

    @Test
    void poste_code_blanc_refuse() {
        PosteForm f = new PosteForm();
        f.setCode("   ");
        f.setNom("Trésorerie régionale");
        assertThat(erreursSur(f, "code")).isEqualTo(1);
    }

    @Test
    void poste_valide_sans_violation() {
        PosteForm f = new PosteForm();
        f.setCode("DKR");
        f.setNom("Trésorerie régionale de Dakar");
        f.setRegion("Dakar");
        assertThat(validator.validate(f)).isEmpty();
    }

    @Test
    void agent_matricule_et_nom_obligatoires() {
        AgentForm f = new AgentForm();
        assertThat(erreursSur(f, "matricule")).isEqualTo(1);
        assertThat(erreursSur(f, "nom")).isEqualTo(1);
    }

    @Test
    void agent_email_invalide_refuse() {
        AgentForm f = new AgentForm();
        f.setMatricule("111111Z");
        f.setNom("Diop");
        f.setEmail("pas-un-email");
        assertThat(erreursSur(f, "email")).isEqualTo(1);
    }

    @Test
    void agent_valide_sans_violation() {
        AgentForm f = new AgentForm();
        f.setMatricule("111111Z");
        f.setNom("Diop");
        f.setPrenom("Awa");
        f.setEmail("awa.diop@dgcpt.sn");
        assertThat(validator.validate(f)).isEmpty();
    }
}
