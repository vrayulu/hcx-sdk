package io.hcxprotocol.validator;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CoverageEligibilityRequest;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StructureDefinition;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

public class HCXFHIRValidator {

    private static HCXFHIRValidator instance = null;

    private FhirValidator validator = null;

    private HCXFHIRValidator() throws Exception {
        FhirContext fhirContext = FhirContext.forR4();

        // Create a chain that will hold the validation modules
        ValidationSupportChain supportChain = new ValidationSupportChain();

        // DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
        // even if we are using custom profiles, since those profiles will derive from the base
        // definitions.
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(fhirContext);
        supportChain.addValidationSupport(defaultSupport);

        // This module supplies several code systems that are commonly used in validation
        supportChain.addValidationSupport(new CommonCodeSystemsTerminologyService(fhirContext));

        // This module implements terminology services for in-memory code validation
        supportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(fhirContext));

        IParser parser = fhirContext.newJsonParser();
        String hcxIGBasePath = "https://ig.hcxprotocol.io/v0.7/";
        String nrcesIGBasePath = "https://nrces.in/ndhm/fhir/r4/";
        // test: load HL7 base definition
        //StructureDefinition sdCoverageEligibilityRequest = (StructureDefinition) parser.parseResource(new URL("http://hl7.org/fhir/R4/coverageeligibilityrequest.profile.json").openStream());
        StructureDefinition sdCoverageEligibilityRequest = (StructureDefinition) parser.parseResource(new URL(hcxIGBasePath + "StructureDefinition-CoverageEligibilityRequest.json").openStream());
        StructureDefinition sdCoverageEligibilityResponse = (StructureDefinition) parser.parseResource(new URL(hcxIGBasePath + "StructureDefinition-CoverageEligibilityResponse.json").openStream());
        StructureDefinition sdClaim = (StructureDefinition) parser.parseResource(new URL(hcxIGBasePath + "StructureDefinition-Claim.json").openStream());
        StructureDefinition sdNRCESPatient = (StructureDefinition) parser.parseResource(new URL(nrcesIGBasePath + "StructureDefinition-Patient.json").openStream());

        // Create a PrePopulatedValidationSupport which can be used to load custom definitions.
        PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(fhirContext);
        prePopulatedSupport.addStructureDefinition(sdCoverageEligibilityRequest);
        prePopulatedSupport.addStructureDefinition(sdCoverageEligibilityResponse);
        prePopulatedSupport.addStructureDefinition(sdClaim);
        prePopulatedSupport.addStructureDefinition(sdNRCESPatient);

        // Add the custom definitions to the chain
        supportChain.addValidationSupport(prePopulatedSupport);
        CachingValidationSupport cache = new CachingValidationSupport(supportChain);

        // Create a validator using the FhirInstanceValidator module.
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(cache);
        this.validator = fhirContext.newValidator().registerValidatorModule(validatorModule);
    }

    private static HCXFHIRValidator getInstance() throws Exception {
        if (null == instance) 
            instance = new HCXFHIRValidator();

        return instance;
    }

    public static FhirValidator getValidator() throws Exception {
        return getInstance().validator;
    }

    public static void main(String[] args) {

        try {
            FhirValidator validator = HCXFHIRValidator.getValidator();
            
            CoverageEligibilityRequest cer = new CoverageEligibilityRequest();
            cer.setId(UUID.randomUUID().toString()); 
            cer.setStatus(CoverageEligibilityRequest.EligibilityRequestStatus.ACTIVE);
            cer.addPurpose(CoverageEligibilityRequest.EligibilityRequestPurpose.DISCOVERY);
            cer.setCreated(new Date());

            /*Patient patientResource = new Patient();
            patientResource.setId("P1001");
            patientResource.addName().setFamily("Simpson").addGiven("Homer");
            patientResource.setGender(Enumerations.AdministrativeGender.MALE);

            Reference patientRef = new Reference();
            patientRef.setResource(patientResource);
            cer.setPatient(patientRef);*/

            ValidationResult result = validator.validateWithResult(cer);
            for (SingleValidationMessage next : result.getMessages()) {
                System.out.println(next.getLocationString() + " -- " + next.getMessage());
            }

            Claim claim = new Claim();
            claim.setId(UUID.randomUUID().toString());
            result = validator.validateWithResult(claim);
            for (SingleValidationMessage next : result.getMessages()) {
                System.out.println(next.getLocationString() + " -- " + next.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
