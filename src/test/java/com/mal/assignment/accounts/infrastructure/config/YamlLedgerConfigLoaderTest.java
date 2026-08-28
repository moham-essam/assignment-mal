package com.mal.assignment.accounts.infrastructure.config;

import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YamlLedgerConfigLoaderTest {

    @Test
    void nullInputIsMissingConfig() {
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new YamlLedgerConfigLoader().load(null)
        );
        assertEquals(FailureReason.MISSING_LEDGER_CONFIG, error.error().reason());
    }

    @Test
    void missingLedgerSectionIsMissingConfig() {
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new YamlLedgerConfigLoader().load(yaml("other: true\n"))
        );
        assertEquals(FailureReason.MISSING_LEDGER_CONFIG, error.error().reason());
    }

    @Test
    void unknownRoundingModeIsRejected() {
        String document = """
                ledger:
                  window-days: 6
                  rounding-mode: BANANAS
                  currencies:
                    AED: { scale: 2 }
                    BHD: { scale: 3 }
                  interest:
                    daily-rate-numerator: 4
                    daily-rate-denominator: 10000
                    capitalization-day: 6
                  overdraft:
                    fees-in-minor-units:
                      AED: 2500
                      BHD: 25000
                  accounts:
                    - id: ACC-001
                      currency: AED
                      opening-balance-in-minor-units: 0
                """;
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new YamlLedgerConfigLoader().load(yaml(document))
        );
        assertEquals(FailureReason.UNSUPPORTED_ROUNDING_MODE, error.error().reason());
    }

    private static ByteArrayInputStream yaml(String document) {
        return new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8));
    }
}
