package org.dbp.comandos;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.dbp.util.CapitulosEnDirectorios;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
@Component
public class PeliculasComando implements CommandMarker {

	private static final String path="Z:\\series";
	private CapitulosEnDirectorios capitulosEnDirectorios = CapitulosEnDirectorios.istancia(Paths.get(path));
	@CliAvailabilityIndicator({"pl establecer","pl path","pl rutas"})
	public boolean isSimpleAvailable() {
		return true;
	}
	
	
	@CliCommand(value = "pl establecer", help = "Es para establecer el path")
	public String establecerPath(
		@CliOption(key = { "path" }, mandatory = true, help = "La path que vamos a establecer") final String path
			) {		
		capitulosEnDirectorios.setPathBase(Paths.get(path));
		return "Se ha establecido la path"+path;
	}
	
	@CliCommand(value = "pl path", help = "Vemos el path establecido")
	public String verPath(){
		return this.capitulosEnDirectorios.getPathBase().toString();
	}
	@CliCommand(value = "pl rutas", help = "Canditas a mover fichas")
	public String candidatos() throws IOException{
		List<Path> rutas =capitulosEnDirectorios.procesarFicheros(false,false);
		return rutas.stream().map(ruta->ruta.toString()+"\n").reduce("", String::concat);
	}

	@CliCommand(value = "pl mover", help = "Mover las carpetas")
	public String mover() throws IOException{
		List<Path> rutas=capitulosEnDirectorios.procesarFicheros(true,true);
		return "Elementos movidos "+rutas.size();
	}
}
