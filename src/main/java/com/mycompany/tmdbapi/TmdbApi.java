import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tmdbapi.Movie;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TmdbApi {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String apiKey = "faf14347183508ec4b12f0ba0fd144c2";
        String apiUrl = "https://api.themoviedb.org/3/search/movie";
        String genreApiUrl = "https://api.themoviedb.org/3/genre/movie/list";

        List<Movie> movieList = new ArrayList<>();

        while (true) {
            System.out.println("Menú:");
            System.out.println("1. Buscar películas");
            System.out.println("2. Mostrar películas");
            System.out.println("3. Salir");
            System.out.println("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();

            if (opcion == 1) {
                System.out.println("Ingrese el título de la película que desea buscar: ");
                String movieTitle = scanner.nextLine();

                try {
                    String encodedMovieTitle = URLEncoder.encode(movieTitle, "UTF-8");
                    String fullUrl = apiUrl + "?api_key=" + apiKey + "&query=" + encodedMovieTitle;

                    HttpClient httpClient = HttpClients.createDefault();
                    HttpGet httpGet = new HttpGet(fullUrl);
                    HttpResponse response = httpClient.execute(httpGet);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    StringBuilder responseText = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseText.append(line);
                    }

                    JsonObject jsonResponse = JsonParser.parseString(responseText.toString()).getAsJsonObject();
                    JsonArray results = jsonResponse.getAsJsonArray("results");
                    results.forEach(element -> {
                        JsonObject movieData = element.getAsJsonObject();
                        Movie movie = new Movie();
                        movie.setTitle(movieData.get("title").getAsString());
                        movie.setVoteAverage(movieData.get("vote_average").getAsDouble());

                        JsonArray genreIds = movieData.getAsJsonArray("genre_ids");
                        List<String> genres = new ArrayList<>();
                        for (JsonElement genreId : genreIds) {
                            int id = genreId.getAsInt();
                            String genre = null;
                            try {
                                genre = getGenreName(apiKey, id);
                            } catch (IOException ex) {
                                Logger.getLogger(TmdbApi.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            genres.add(genre);
                        }
                        movie.setGenres(genres);

                        // Obtener las reseñas de la película
                        List<String> reviews = null;
                        try {
                            reviews = getMovieReviews(apiKey, movieData.get("id").getAsInt());
                        } catch (IOException ex) {
                            Logger.getLogger(TmdbApi.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        movie.setReviews(reviews);

                        movieList.add(movie);
                    });

                    System.out.println("Películas encontradas y almacenadas en la lista.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (opcion == 2) {
                System.out.println("Películas encontradas:");
                movieList.forEach(movie -> {
                    System.out.println("Título: " + movie.getTitle());
                    System.out.println("Calificación: " + movie.getVoteAverage());
                    System.out.println("Género(s): " + String.join(", ", movie.getGenres()));
                    System.out.println("Reseñas:");
                    for (String review : movie.getReviews()) {
                        System.out.println(review);
                    }
                    System.out.println();
                });
            } else if (opcion == 3) {
                System.out.println("Saliendo...");
                break;
            } else {
                System.out.println("Opción no válida. Por favor, seleccione una opción válida.");
            }
        }
    }

    // Función para obtener el nombre del género a partir de su ID
    private static String getGenreName(String apiKey, int genreId) throws IOException {
        String genreApiUrl = "https://api.themoviedb.org/3/genre/movie/list";
        String fullUrl = genreApiUrl + "?api_key=" + apiKey;

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(fullUrl);
        HttpResponse response = httpClient.execute(httpGet);

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder responseText = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }

        JsonObject jsonResponse = JsonParser.parseString(responseText.toString()).getAsJsonObject();
        JsonArray genres = jsonResponse.getAsJsonArray("genres");

        for (int i = 0; i < genres.size(); i++) {
            JsonObject genreData = genres.get(i).getAsJsonObject();
            if (genreData.get("id").getAsInt() == genreId) {
                return genreData.get("name").getAsString();
            }
        }

        return "Desconocido";
    }

    // Función para obtener las reseñas de una película
    private static List<String> getMovieReviews(String apiKey, int movieId) throws IOException {
        String reviewsApiUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/reviews";
        String fullUrl = reviewsApiUrl + "?api_key=" + apiKey;

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(fullUrl);
        HttpResponse response = httpClient.execute(httpGet);

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder responseText = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }

        JsonObject jsonResponse = JsonParser.parseString(responseText.toString()).getAsJsonObject();
        JsonArray results = jsonResponse.getAsJsonArray("results");

        List<String> reviews = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JsonObject reviewData = results.get(i).getAsJsonObject();
            reviews.add(reviewData.get("content").getAsString());
        }

        return reviews;
    }
}
