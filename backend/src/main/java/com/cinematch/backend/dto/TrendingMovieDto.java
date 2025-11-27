package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // δημιουργεί getters/setters/toString αυτόματα
@AllArgsConstructor
@NoArgsConstructor
public class TrendingMovieDto {

    private Long id;
    private String title;        // Τίτλος ταινίας
    private String overview;     // Περιγραφή
    private String posterPath;   // Poster URL
    private double popularity;   // Βαθμός δημοτικότητας
    private String releaseDate;  // Ημερομηνία κυκλοφορίας

}

