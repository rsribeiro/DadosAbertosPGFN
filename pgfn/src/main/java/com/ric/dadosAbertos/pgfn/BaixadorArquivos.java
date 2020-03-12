package com.ric.dadosAbertos.pgfn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BaixadorArquivos {
	private static final String ARQUIVO_FGTS = "http://dadosabertos.pgfn.gov.br/Dados_abertos_FGTS.zip";
	private static final String ARQUIVO_NAO_PREVIDENCIARIO = "http://dadosabertos.pgfn.gov.br/Dados_abertos_Nao_Previdenciario.zip";
	private static final String ARQUIVO_PREVIDENCIARIO = "http://dadosabertos.pgfn.gov.br/Dados_abertos_Previdenciario.zip";

	public void baixarArquivos(Path dirFGTS, Path dirNaoPrevidenciario, Path dirPrevidenciario) {
		HttpClient httpClient = HttpClient.newBuilder().build();
		ExecutorService executor = Executors.newFixedThreadPool(3);

		CompletableFuture<Void> futureFGTS = baixarExtrairZipAsync(dirFGTS, ARQUIVO_FGTS, httpClient, executor);
		CompletableFuture<Void> futureNaoPrevidenciario = baixarExtrairZipAsync(dirNaoPrevidenciario, ARQUIVO_NAO_PREVIDENCIARIO, httpClient, executor);
		CompletableFuture<Void> futurePrevidenciario = baixarExtrairZipAsync(dirPrevidenciario, ARQUIVO_PREVIDENCIARIO, httpClient, executor);

		CompletableFuture.allOf(futureFGTS, futureNaoPrevidenciario, futurePrevidenciario).join();
		executor.shutdown();
	}

	public Path baixarExtrairZip(String urlStr, HttpClient httpClient) throws IOException, InterruptedException {
		Path tempDir = Files.createTempDirectory("pgfn_");
		System.out.println("Arquivo " + urlStr + " => " + tempDir);

		long t0 = System.nanoTime();

		HttpRequest request = HttpRequest
				.newBuilder()
				.uri(URI.create(urlStr))
				.GET()
				.build();

		HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());

		try (InputStream is = response.body();) {
			extrairZip(tempDir, is);

			double deltaT = (System.nanoTime() - t0)/1E9;
			System.out.println("Baixado em " + deltaT + "s.");

			return tempDir;
		}
	}

	public CompletableFuture<Void> baixarExtrairZipAsync(Path dir, String urlStr, HttpClient httpClient, Executor executor) {
		HttpRequest request = HttpRequest
				.newBuilder()
				.uri(URI.create(urlStr))
				.GET()
				.build();

		System.out.println("Arquivo " + urlStr + " => " + dir);
		return httpClient.sendAsync(request, BodyHandlers.ofInputStream())
		.thenApplyAsync(HttpResponse::body, executor)
		.thenAcceptAsync(is -> extrairZip(dir, is));
	}

	public void extrairZip(Path tempDir, InputStream is) {
		long t0 = System.nanoTime();

		System.out.println("Baixando arquivo e extraindo em " + tempDir);
		try (ZipInputStream zis = new ZipInputStream(is);) {
			ZipEntry entry = zis.getNextEntry();
			while (entry != null) {
				Path newFile = newFile(tempDir, entry);

				try (OutputStream os = Files.newOutputStream(newFile);) {
					zis.transferTo(os);
				}

				zis.closeEntry();
				entry = zis.getNextEntry();
			}

			double deltaT = (System.nanoTime() - t0)/1E9;
			System.out.println("Arquivo extraído em " + deltaT + "s.");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//https://www.baeldung.com/java-compress-and-uncompress
	//https://snyk.io/research/zip-slip-vulnerability#java
	private Path newFile(Path destinationDir, ZipEntry zipEntry) throws IOException {
        Path destFile = destinationDir.resolve(zipEntry.getName());

        destinationDir = destinationDir.toAbsolutePath().normalize();
        destFile = destFile.toAbsolutePath().normalize();

        if (!destFile.startsWith(destinationDir)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
