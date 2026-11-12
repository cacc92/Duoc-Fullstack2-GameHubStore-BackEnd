package cl.gamehub.categorias.config;

import cl.gamehub.categorias.model.Categoria;
import cl.gamehub.categorias.repository.CategoriaRepository;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Carga las 7 categorías de ejemplo si la base está vacía (sus id quedan del 1 al 7). */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final CategoriaRepository categorias;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, CategoriaRepository categorias) {
        this.habilitado = habilitado;
        this.categorias = categorias;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || categorias.count() > 0) {
            return;
        }
        DatosDeEjemplo.CATEGORIAS.forEach(c -> categorias.save(new Categoria(c.nombre())));
        log.info("Datos de ejemplo cargados: {} categorías.", categorias.count());
    }
}
