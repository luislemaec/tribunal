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
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Named
@SessionScoped
public class GuestPreferences implements Serializable {

    @Setter
    @Getter
    private String theme = "blue";

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
        menuThemes.add(new MenuTheme("Amber", "amber", "#f66b0b", "#efd417"));
        menuThemes.add(new MenuTheme("Blue", "blue", "#2872B4", "#26BED0"));
        menuThemes.add(new MenuTheme("Blue-Grey", "bluegrey", "#16222A", "#3A6073"));
        menuThemes.add(new MenuTheme("Cyan", "cyan", "#12AABD", "#C4C988"));
        menuThemes.add(new MenuTheme("Dark-Blue", "darkblue", "#4b6cb7", "#182848"));
        menuThemes.add(new MenuTheme("Deep-Orange", "deeporange", "#FF2525", "#FFA43B"));
        menuThemes.add(new MenuTheme("Deep-Purple", "deeppurple", "#5023A0", "#7A318D"));
        menuThemes.add(new MenuTheme("Green", "green", "#07b750", "#c7d41b"));
        menuThemes.add(new MenuTheme("Grey", "grey", "#333333", "#5A5D60"));
        menuThemes.add(new MenuTheme("Indigo", "indigo", "#1e469a", "#49a7c1"));
        menuThemes.add(new MenuTheme("Lime", "lime", "#53C018", "#C6D309"));
        menuThemes.add(new MenuTheme("Mojito", "mojito", "#1D976C", "#93F9B9"));
        menuThemes.add(new MenuTheme("Pink", "pink", "#e02365", "#db3c06"));
        menuThemes.add(new MenuTheme("Purple", "purple", "#B721FF", "#21D4FD"));
        menuThemes.add(new MenuTheme("Yellow", "yellow", "#F39C05", "#F3C704"));
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
