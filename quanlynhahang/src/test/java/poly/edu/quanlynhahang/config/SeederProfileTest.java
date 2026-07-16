package poly.edu.quanlynhahang.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;

import poly.edu.quanlynhahang.service.DatabaseInitService;
import poly.edu.quanlynhahang.service.DemoDataSeeder;
import poly.edu.quanlynhahang.service.MenuExpansionSeeder;
import poly.edu.quanlynhahang.service.MenuInitService;
import poly.edu.quanlynhahang.service.TextEncodingRepairSeeder;

class SeederProfileTest {

    @Test
    void businessSeedersOnlyRunInDemoProfile() {
        assertProfile(MenuInitService.class, "demo");
        assertProfile(MenuExpansionSeeder.class, "demo");
        assertProfile(DemoDataSeeder.class, "demo");
    }

    @Test
    void repairAndBootstrapRequireExplicitProfiles() {
        assertProfile(TextEncodingRepairSeeder.class, "repair");
        assertProfile(DatabaseInitService.class, "bootstrap-admin");
    }

    private void assertProfile(Class<?> type, String expected) {
        Profile profile = AnnotatedElementUtils.findMergedAnnotation(type, Profile.class);
        assertArrayEquals(new String[]{expected}, profile.value());
    }
}
