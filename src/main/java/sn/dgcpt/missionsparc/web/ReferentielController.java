package sn.dgcpt.missionsparc.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sn.dgcpt.missionsparc.domain.CategorieCable;
import sn.dgcpt.missionsparc.domain.CategorieMateriel;
import sn.dgcpt.missionsparc.domain.Logiciel;
import sn.dgcpt.missionsparc.domain.TypeMateriel;
import sn.dgcpt.missionsparc.repository.CategorieCableRepository;
import sn.dgcpt.missionsparc.repository.CategorieMaterielRepository;
import sn.dgcpt.missionsparc.repository.LogicielRepository;

/** Administration des référentiels paramétrables : logiciels, catégories de câble, types de matériel (cahier §3.8). */
@Controller
@RequestMapping("/referentiels")
public class ReferentielController {

    private final LogicielRepository logicielRepo;
    private final CategorieCableRepository cableRepo;
    private final CategorieMaterielRepository typeRepo;

    public ReferentielController(LogicielRepository logicielRepo, CategorieCableRepository cableRepo,
                                 CategorieMaterielRepository typeRepo) {
        this.logicielRepo = logicielRepo;
        this.cableRepo = cableRepo;
        this.typeRepo = typeRepo;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("logiciels", logicielRepo.findAll());
        model.addAttribute("cables", cableRepo.findAll());
        model.addAttribute("types", typeRepo.findByActifTrueOrderByLibelle());
        return "referentiels";
    }

    @PostMapping("/logiciels")
    public String ajouterLogiciel(@RequestParam String nom, RedirectAttributes ra) {
        String n = nom == null ? "" : nom.trim();
        if (n.isEmpty()) ra.addFlashAttribute("erreur", "Le nom du logiciel est obligatoire.");
        else if (logicielRepo.findByNom(n).isPresent()) ra.addFlashAttribute("erreur", "Le logiciel « " + n + " » existe déjà.");
        else {
            Logiciel l = new Logiciel();
            l.setNom(n);
            l.setActif(true);
            logicielRepo.save(l);
            ra.addFlashAttribute("message", "Logiciel « " + n + " » ajouté.");
        }
        return "redirect:/referentiels";
    }

    @PostMapping("/logiciels/{id}")
    public String renommerLogiciel(@PathVariable Integer id, @RequestParam String nom, RedirectAttributes ra) {
        String n = nom == null ? "" : nom.trim();
        Logiciel l = logicielRepo.findById(id).orElse(null);
        if (l == null) ra.addFlashAttribute("erreur", "Logiciel introuvable.");
        else if (n.isEmpty()) ra.addFlashAttribute("erreur", "Le nom est obligatoire.");
        else { l.setNom(n); logicielRepo.save(l); ra.addFlashAttribute("message", "Logiciel renommé."); }
        return "redirect:/referentiels";
    }

    @PostMapping("/logiciels/{id}/supprimer")
    public String supprimerLogiciel(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            logicielRepo.deleteById(id);
            logicielRepo.flush();
            ra.addFlashAttribute("message", "Logiciel supprimé.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("erreur", "Suppression impossible : ce logiciel est utilisé par du matériel.");
        }
        return "redirect:/referentiels";
    }

    @PostMapping("/cables")
    public String ajouterCable(@RequestParam String libelle, RedirectAttributes ra) {
        String n = libelle == null ? "" : libelle.trim();
        if (n.isEmpty()) ra.addFlashAttribute("erreur", "Le libellé de la catégorie est obligatoire.");
        else if (cableRepo.findByLibelle(n).isPresent()) ra.addFlashAttribute("erreur", "La catégorie « " + n + " » existe déjà.");
        else {
            CategorieCable cc = new CategorieCable();
            cc.setLibelle(n);
            cableRepo.save(cc);
            ra.addFlashAttribute("message", "Catégorie « " + n + " » ajoutée.");
        }
        return "redirect:/referentiels";
    }

    @PostMapping("/cables/{id}")
    public String renommerCable(@PathVariable Integer id, @RequestParam String libelle, RedirectAttributes ra) {
        String n = libelle == null ? "" : libelle.trim();
        CategorieCable cc = cableRepo.findById(id).orElse(null);
        if (cc == null) ra.addFlashAttribute("erreur", "Catégorie introuvable.");
        else if (n.isEmpty()) ra.addFlashAttribute("erreur", "Le libellé est obligatoire.");
        else { cc.setLibelle(n); cableRepo.save(cc); ra.addFlashAttribute("message", "Catégorie renommée."); }
        return "redirect:/referentiels";
    }

    @PostMapping("/cables/{id}/supprimer")
    public String supprimerCable(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            cableRepo.deleteById(id);
            cableRepo.flush();
            ra.addFlashAttribute("message", "Catégorie supprimée.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("erreur", "Suppression impossible : cette catégorie est utilisée par une mission.");
        }
        return "redirect:/referentiels";
    }

    // ---------- Types de matériel (paramétrables) ----------

    @PostMapping("/types")
    public String ajouterType(@RequestParam String libelle, @RequestParam String prefixe, RedirectAttributes ra) {
        String lib = libelle == null ? "" : libelle.trim();
        String pre = prefixe == null ? "" : prefixe.trim().toUpperCase();
        if (lib.isEmpty() || pre.isEmpty()) ra.addFlashAttribute("erreur", "Le libellé et le préfixe sont obligatoires.");
        else if (!pre.matches("[A-Z]{2,4}")) ra.addFlashAttribute("erreur", "Le préfixe doit comporter 2 à 4 lettres.");
        else if (typeRepo.findByLibelle(lib).isPresent()) ra.addFlashAttribute("erreur", "Le type « " + lib + " » existe déjà.");
        else if (typeRepo.findByPrefixe(pre).isPresent()) ra.addFlashAttribute("erreur", "Le préfixe « " + pre + " » est déjà utilisé.");
        else {
            CategorieMateriel t = new CategorieMateriel();
            t.setLibelle(lib);
            t.setPrefixe(pre);
            t.setFamille(TypeMateriel.AUTRE);
            t.setActif(true);
            t.setSysteme(false);
            typeRepo.save(t);
            ra.addFlashAttribute("message", "Type « " + lib + " » ajouté.");
        }
        return "redirect:/referentiels";
    }

    @PostMapping("/types/{id}")
    public String modifierType(@PathVariable Integer id, @RequestParam String libelle, @RequestParam String prefixe,
                               RedirectAttributes ra) {
        String lib = libelle == null ? "" : libelle.trim();
        String pre = prefixe == null ? "" : prefixe.trim().toUpperCase();
        CategorieMateriel t = typeRepo.findById(id).orElse(null);
        if (t == null) ra.addFlashAttribute("erreur", "Type introuvable.");
        else if (t.isSysteme()) ra.addFlashAttribute("erreur", "Les types système ne sont pas modifiables.");
        else if (lib.isEmpty() || pre.isEmpty()) ra.addFlashAttribute("erreur", "Le libellé et le préfixe sont obligatoires.");
        else if (!pre.matches("[A-Z]{2,4}")) ra.addFlashAttribute("erreur", "Le préfixe doit comporter 2 à 4 lettres.");
        else if (typeRepo.findByLibelle(lib).filter(a -> !a.getId().equals(id)).isPresent())
            ra.addFlashAttribute("erreur", "Le type « " + lib + " » existe déjà.");
        else if (typeRepo.findByPrefixe(pre).filter(a -> !a.getId().equals(id)).isPresent())
            ra.addFlashAttribute("erreur", "Le préfixe « " + pre + " » est déjà utilisé.");
        else { t.setLibelle(lib); t.setPrefixe(pre); typeRepo.save(t); ra.addFlashAttribute("message", "Type modifié."); }
        return "redirect:/referentiels";
    }

    @PostMapping("/types/{id}/supprimer")
    public String supprimerType(@PathVariable Integer id, RedirectAttributes ra) {
        CategorieMateriel t = typeRepo.findById(id).orElse(null);
        if (t == null) { ra.addFlashAttribute("erreur", "Type introuvable."); return "redirect:/referentiels"; }
        if (t.isSysteme()) { ra.addFlashAttribute("erreur", "Les types système ne sont pas supprimables."); return "redirect:/referentiels"; }
        try {
            typeRepo.deleteById(id);
            typeRepo.flush();
            ra.addFlashAttribute("message", "Type supprimé.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("erreur", "Suppression impossible : ce type est utilisé par du matériel.");
        }
        return "redirect:/referentiels";
    }
}
