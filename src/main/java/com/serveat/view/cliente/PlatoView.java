package com.serveat.view.cliente;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.menu.Categoria;
import com.serveat.service.PlatoService;
import com.serveat.util.MapperUtils;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "carta", layout = MainLayout.class)
@AnonymousAllowed
public class PlatoView extends VerticalLayout {

    public PlatoView(PlatoService platoService) {
        addClassName("carta-view");

        H1 titulo = new H1("Nuestra Carta");
        titulo.addClassName("carta-title");
        add(titulo);

        Accordion accordion = new Accordion();
        accordion.addClassName("carta-accordion");

        Map<Categoria, List<Plato>> platosPorCategoria = platoService.listarConCategoria()
                .stream()
                .collect(Collectors.groupingBy(Plato::getCategoria));

        for (Map.Entry<Categoria, List<Plato>> entry : platosPorCategoria.entrySet()) {
            Categoria categoria = entry.getKey();
            List<Plato> platos = entry.getValue();

            VerticalLayout contenedor = new VerticalLayout();
            contenedor.setSpacing(true);
            contenedor.setPadding(false);
            contenedor.setAlignItems(Alignment.CENTER);

            for (Plato p : platos) {
                Card card = new Card();
                card.addClassName("plato-card");
                card.getElement().getThemeList().add("outlined");

                String url = (p.getImagen() == null || p.getImagen().isBlank())
                        ? "/images/default-placeholder.png"
                        : p.getImagen();

                Image img = new Image(url, "Imagen de " + p.getNombre());
                img.addClassName("plato-imagen");
                card.setMedia(img);

                Div tituloPlato = new Div(p.getNombre());
                tituloPlato.addClassName("plato-nombre");
                card.setTitle(tituloPlato);

                Div precio = new Div(MapperUtils.formatEuro(p.getPrecio()));
                precio.addClassName("plato-precio");
                card.setSubtitle(precio);

                if (p.getDescripcion() != null && !p.getDescripcion().isBlank()) {
                    Paragraph desc = new Paragraph(p.getDescripcion());
                    desc.addClassName("plato-descripcion");
                    card.add(desc);
                }

                Span badge = new Span(categoria.getNombre());
                badge.addClassName("plato-categoria");
                card.addToFooter(badge);

                contenedor.add(card);
            }

            accordion.add(categoria.getNombre(), contenedor);
        }

        accordion.close();
        add(accordion);
    }
}