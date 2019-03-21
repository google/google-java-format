class B124394008 {
  {
    LocalReviewProto.Builder newReview =
        readProfileResponse
            .flatMap(authorExtractor::extract)
            .map(luReviewsExtractor::toLocalReviewProto)
            .orElse(LocalReviewProto.getDefaultInstance())
            .toBuilder();
  }
}
