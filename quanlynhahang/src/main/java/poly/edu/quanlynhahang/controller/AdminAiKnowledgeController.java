package poly.edu.quanlynhahang.controller;

import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.AiKnowledgeSource;
import poly.edu.quanlynhahang.service.AiKnowledgeService;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import poly.edu.quanlynhahang.entity.AiBrandProfile;
import poly.edu.quanlynhahang.entity.AiFaqExample;
import java.io.IOException;

@RestController
@RequestMapping("/api/admin/ai/knowledge")
public class AdminAiKnowledgeController {
    private final AiKnowledgeService service;
    public AdminAiKnowledgeController(AiKnowledgeService service) { this.service = service; }
    @GetMapping public List<AiKnowledgeSource> all() { return service.all(); }
    @PostMapping public AiKnowledgeSource create(@RequestBody AiKnowledgeSource source) { return service.save(source); }
    @PutMapping("/{id}") public AiKnowledgeSource update(@PathVariable Long id, @RequestBody AiKnowledgeSource source) { return service.update(id, source); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { service.delete(id); }
    @PostMapping(value="/upload", consumes="multipart/form-data")
    public AiKnowledgeSource upload(@RequestParam MultipartFile file, @RequestParam(required=false) String title) throws IOException { return service.upload(file, title); }
    @GetMapping("/brand") public AiBrandProfile brand() { return service.brand(); }
    @PutMapping("/brand") public AiBrandProfile saveBrand(@RequestBody AiBrandProfile profile) { return service.saveBrand(profile); }
    @GetMapping("/faqs") public List<AiFaqExample> faqs() { return service.faqs(); }
    @PostMapping("/faqs") public AiFaqExample createFaq(@RequestBody AiFaqExample faq) { return service.saveFaq(faq); }
    @PutMapping("/faqs/{id}") public AiFaqExample updateFaq(@PathVariable Long id,@RequestBody AiFaqExample faq) { return service.updateFaq(id,faq); }
    @DeleteMapping("/faqs/{id}") public void deleteFaq(@PathVariable Long id) { service.deleteFaq(id); }
}
