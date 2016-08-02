package org.dbp.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapitulosEnDirectorios {

	private static final int SIN_TEMPORADA = -1;

	private static Logger logger = LoggerFactory.getLogger(CapitulosEnDirectorios.class);
	
	private final Integer profundidad;
	private Path pathBase;
	private final String extensiones[];
	
	private static final String EXTENSIONES_PELICULA[]={".mkv",".avi"};
	private static final String patronPath ="[\\w\\s\\.\\(\\)ραινσϊ]+-[\\w\\s\\.\\(\\)ραινσϊ]+\\[[\\w\\s\\.\\(\\)ραινσϊ]+\\]\\[[\\w\\s\\.\\(\\)ραινσϊ]+\\]\\[[\\w\\s\\.\\(\\)ραινσϊ]+\\]";
	private static final String patronPalabras = "\\[[\\w\\s\\.\\(\\)ραινσϊ]+\\]";
	private final Pattern patternPath;
	private final Pattern patternPalabras;
	private final Pattern patternNumero;
	public static final CapitulosEnDirectorios istancia(final Path pathBase){
		return new CapitulosEnDirectorios(5,pathBase);
	}


	
	private CapitulosEnDirectorios(Integer profundidad,final Path pathBase) {
		super();
		this.profundidad=profundidad;
		this.pathBase = pathBase;
		this.extensiones=EXTENSIONES_PELICULA;
		this.patternPath = Pattern.compile(patronPath);
		this.patternPalabras = Pattern.compile(patronPalabras);
		this.patternNumero = Pattern.compile("\\d+");
	}
	
	public CapitulosEnDirectorios setPathBase(final Path pathBase){
		this.pathBase=pathBase;
		return this;
	}

	public Path getPathBase(){
		return this.pathBase;
	}
	
	public List<Path> procesarFicheros(Boolean mover,Boolean eliminar) throws IOException{
		final List<Path> rutas=new ArrayList<Path>();
		try(Stream<Path> ficheros = Files.find(this.pathBase,this.profundidad,
			(path,attr)->path.getNameCount()==this.profundidad && attr.isRegularFile() 
				&& Arrays.stream(this.extensiones).filter(ext->String.valueOf(path).endsWith(ext)).findAny().isPresent()
				)){
				Map<Path,List<Path>> organizado = ficheros
				.collect(groupingBy(p->obtenerDirectorioTemporada(p),mapping((Path p)->p,toList())));
				
				for(Map.Entry<Path,List<Path>> entrada:organizado.entrySet()){
					logger.info("DT [{}]",entrada.getKey());
					rutas.add(entrada.getKey());
					List<Capitulo> capitulos=ponerNombreDirectorio(entrada.getKey(),entrada.getValue());
					for(Capitulo capitulo:capitulos){
						if(mover)System.out.println("Capitulo:"+capitulo.capitulo);
						if(mover)moverCapitulo(capitulo);
						if(eliminar)eliminarDirectorio(capitulo);
					}
				}
			}
		return rutas;
	}

	private Path obtenerDirectorioTemporada(final Path p) {
		return p.resolve(String.valueOf(p.getRoot()).concat(String.valueOf(p.subpath(0, p.getNameCount()-2))));
	}

	private List<Capitulo> ponerNombreDirectorio(final Path padre,final List<Path> ficherosTemporada){
		return ficherosTemporada.stream()
			.map(path->procesarDirectorio(path))
			.filter(capitulo->isValido(capitulo))
			.map(capitulo->{
				capitulo.pathDestino=pathMoverNuevoFichero(padre, capitulo);
				return capitulo;
			}).collect(toList());
	}
	
	public enum TipoResolucion{
		HD720p(720),HD1080p(1080),DESCONOCIDA(-1)
		;
		private Integer resolucion;

		private TipoResolucion(Integer resolucion) {
			this.resolucion = resolucion;
		}

		private static TipoResolucion getResolucion(final Integer resolucion){
			return Arrays.stream(TipoResolucion.values())
					.filter(res->res.resolucion.equals(resolucion))
						.findAny().orElse(TipoResolucion.DESCONOCIDA);
		}
		
	}
	
	private class Capitulo{
		private String titulo;
		private Integer temporada;
		private TipoResolucion tipo;
		private String capitulo;
		private Path patOrigen;
		private Path pathDestino;
	}
	private Capitulo procesarDirectorio(Path path){
		String nombre=String.valueOf(path.getParent().getFileName());
		Capitulo capitulo = new Capitulo();
		capitulo.patOrigen=path;
		Matcher matcher = this.patternPath.matcher(nombre);
		if(matcher.find()){
			String sinProcesar = matcher.group();
			String[] partidoAux = sinProcesar.split("-");
			capitulo.titulo = partidoAux[0];
			sinProcesar = partidoAux[1];
			partidoAux=sinProcesar.split(patronPalabras, 2);
			capitulo.temporada=buscarNumero(partidoAux[0]).orElse(SIN_TEMPORADA);
			List<String> palabras = obtenerLasPalabras(sinProcesar);
			capitulo.tipo=TipoResolucion.getResolucion(buscarNumero(palabras.get(0)).orElse(-1));
			capitulo.capitulo=obtenerNumeroCapitulo(capitulo,buscarNumero(palabras.get(1)).orElse(-1));
		}
		return capitulo;
	}

	public Optional<Integer> buscarNumero(String numero){
		Matcher matcherTemporado=patternNumero.matcher(numero);
		if(matcherTemporado.find()){
			return Optional.ofNullable(Integer.valueOf(matcherTemporado.group()));
		}
		return Optional.empty();
	}
	
	private List<String> obtenerLasPalabras(String sinProcesar) {
		Matcher matcherPalabras=patternPalabras.matcher(sinProcesar);
		List<String> palabras = new ArrayList<String>();
		while(matcherPalabras.find()){
			palabras.add(matcherPalabras.group());
		}
		return palabras;
	}
	
	private String obtenerNumeroCapitulo(Capitulo capitulo,Integer capSinProcesar) {
		String numero = "";
		if(capSinProcesar.toString().length()>2 && capSinProcesar.toString().startsWith(capitulo.temporada.toString())){
			numero=capSinProcesar.toString().substring(1);
		}else{
			numero=capSinProcesar.toString();
		}
		return numero;
	}
	

	
	private Path pathMoverNuevoFichero(final Path directorio,final Capitulo capitulo){
		return capitulo.patOrigen.resolve(directorio.toString().concat("\\").concat(nombreOrigen(capitulo)));
	}
	
	private String nombreOrigen(Capitulo capitulo){
		return String.format("%sC%d%s_%s%s"
				, procesarNomber(capitulo.titulo)
				, capitulo.temporada
				, capitulo.capitulo
				, capitulo.tipo
				, procesarExtendion(capitulo.patOrigen)
				);
	}
	
	private String procesarNomber(String nombre){
		return nombre.replaceAll("\\s", "") // Quitar los especacion es blanco
				.replaceAll("\\([\\w\\s\\.\\(\\)ραινσϊ]+\\)", "");// Quitar los parentesis
	}
	
	private Boolean isValido(Capitulo capitulo){
		return capitulo.titulo!=null 
				&& capitulo.temporada!= -1
				&& capitulo.capitulo!= null;
	}
	
	private String procesarExtendion(Path path){
		String nombre=path.getFileName().toString();
		return nombre.substring(nombre.lastIndexOf("."));
	}
	
	private void moverCapitulo(Capitulo capitulo) throws IOException{
		logger.info("Mover O [{}] D [{}]",capitulo.patOrigen, capitulo.pathDestino);
		Files.move(capitulo.patOrigen, capitulo.pathDestino);
	}
	
	private void eliminarDirectorio(Capitulo capitulo) throws IOException{
		logger.info("Eliminar Directorio [{}]",capitulo.patOrigen.getParent());
		capitulo.patOrigen.getParent();
		Files.list(capitulo.patOrigen.getParent()).forEach(path->{
			try{
				Files.delete(path);
			}catch (Exception e) {
				throw new RuntimeException("Se ha producido un error en la path "+path,e);
			}
		});
		Files.delete(capitulo.patOrigen.getParent());
		
	}
}
