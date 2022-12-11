package pt.ulisboa.tecnico.transparency.ca;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.transparency.ca.contract.CAOuterClass.*;

@RestController
@RequestMapping(path = "v2/ca")
public class CaController {

    private final CaService caService;

    @Autowired
    public CaController(CaService caService) {
        this.caService = caService;
    }

    @PostMapping
    public Certificate generateCertificate(@RequestBody CertificateSigningRequest csr){
        return caService.generateCertificate(csr);
    }
}
