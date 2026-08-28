package com.mal.assignment.accounts.infrastructure.config;

import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlLedgerConfigLoader {

    private static final String CLASSPATH_RESOURCE = "/application.yaml";

    public LedgerConfig loadFromClasspath() {
        try (InputStream input = YamlLedgerConfigLoader.class.getResourceAsStream(CLASSPATH_RESOURCE)) {
            if (input == null) {
                throw LedgerDomainException.of(
                        FailureReason.MISSING_LEDGER_CONFIG,
                        "Missing classpath resource " + CLASSPATH_RESOURCE
                );
            }
            return load(input);
        } catch (LedgerDomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw LedgerDomainException.of(
                    FailureReason.CONFIG_LOAD_FAILED,
                    "Failed to load ledger config",
                    ex
            );
        }
    }

    @SuppressWarnings("unchecked")
    public LedgerConfig load(InputStream input) {
        if (input == null) {
            throw LedgerDomainException.of(FailureReason.MISSING_LEDGER_CONFIG, "config input is required");
        }
        try {
            Map<String, Object> root = new Yaml().load(input);
            Map<String, Object> ledger = (Map<String, Object>) root.get("ledger");
            if (ledger == null) {
                throw LedgerDomainException.of(FailureReason.MISSING_LEDGER_CONFIG, "ledger section is required");
            }
            Map<String, Object> currenciesRaw = (Map<String, Object>) ledger.get("currencies");
            Map<String, CurrencyScaleConfig> currencies = new LinkedHashMap<>();
            currenciesRaw.forEach((code, value) -> {
                Map<String, Object> scaleMap = (Map<String, Object>) value;
                currencies.put(code, new CurrencyScaleConfig(asInt(scaleMap.get("scale"))));
            });
            Map<String, Object> interestRaw = (Map<String, Object>) ledger.get("interest");
            Map<String, Object> overdraftRaw = (Map<String, Object>) ledger.get("overdraft");
            Map<String, Object> feesRaw = (Map<String, Object>) overdraftRaw.get("fees-in-minor-units");
            Map<String, Long> fees = new LinkedHashMap<>();
            feesRaw.forEach((code, value) -> fees.put(code, asLong(value)));
            List<Map<String, Object>> accountsRaw = (List<Map<String, Object>>) ledger.get("accounts");
            List<AccountSeedConfig> accounts = new ArrayList<>();
            for (Map<String, Object> row : accountsRaw) {
                accounts.add(new AccountSeedConfig(
                        String.valueOf(row.get("id")),
                        String.valueOf(row.get("currency")),
                        asLong(row.get("opening-balance-in-minor-units"))
                ));
            }
            RoundingMode roundingMode;
            try {
                roundingMode = RoundingMode.valueOf(String.valueOf(ledger.get("rounding-mode")));
            } catch (IllegalArgumentException ex) {
                throw LedgerDomainException.of(
                        FailureReason.UNSUPPORTED_ROUNDING_MODE,
                        "Unknown rounding mode " + ledger.get("rounding-mode")
                );
            }
            return new LedgerConfig(
                    asInt(ledger.get("window-days")),
                    roundingMode,
                    Map.copyOf(currencies),
                    new InterestConfig(
                            asInt(interestRaw.get("daily-rate-numerator")),
                            asInt(interestRaw.get("daily-rate-denominator")),
                            asInt(interestRaw.get("capitalization-day"))
                    ),
                    new OverdraftConfig(Map.copyOf(fees)),
                    List.copyOf(accounts)
            );
        } catch (LedgerDomainException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw LedgerDomainException.of(FailureReason.CONFIG_LOAD_FAILED, "Failed to parse ledger config", ex);
        }
    }

    private static int asInt(Object value) {
        return asLong(value).intValue();
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
