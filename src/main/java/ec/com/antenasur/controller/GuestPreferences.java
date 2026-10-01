/*
   Copyright 2009-2022 PrimeTek.

   Licensed under PrimeFaces Commercial License, Version 1.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   Licensed under PrimeFaces Commercial License, Version 1.0 (the "License");

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Named
@SessionScoped
public class GuestPreferences implements Serializable {

    /** Tema predeterminado oficial del TEC; también es el respaldo ante un valor desconocido. */
    public static final String DEFAULT_THEME = "tribunal";

    @Getter
    private String theme = DEFAULT_THEME;

    @Setter
    @Getter
    private String menuMode = "layout-menu-static";

    @Setter
    @Getter
    private String menuColor = "light";

    @Setter
    @Getter
    private boolean orientationRTL;

    @Setter
    @Getter
    private String inputStyle = "outlined";

    @Getter
    private List<MenuTheme> menuThemes;

    @PostConstruct
    public void init() {
        menuThemes = new ArrayList<>();
        // Único tema oficial del TEC (azul institucional). Se conserva la lista para validar el valor recibido.
        menuThemes.add(new MenuTheme("Tribunal", DEFAULT_THEME, "#034EA2", "#0B2E6B"));
    }

    /**
     * Solo acepta temas del catálogo: un valor desconocido (petición manipulada o tema retirado)
     * vuelve al predeterminado para no pedir un CSS inexistente. Sin consultas ni procesamiento extra.
     */
    public void setTheme(String theme) {
        boolean valido = theme != null && menuThemes != null && menuThemes.stream().anyMatch(t -> t.getFile().equals(theme));
        this.theme = valido ? theme : DEFAULT_THEME;
    }

    public String getInputStyleClass() {
        return this.inputStyle.equals("filled") ? "ui-input-filled" : "";
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public class MenuTheme {

        @Setter
        @Getter
        private String name;

        @Setter
        @Getter
        private String file;

        @Setter
        @Getter
        private String color1;

        @Setter
        @Getter
        private String color2;

    }
}
