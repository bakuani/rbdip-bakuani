package com.rbdip.bookstore.review;

import com.rbdip.bookstore.purchase.OrderPurchasePort;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderPurchasePort orderPurchasePort;

    public ReviewService(ReviewRepository reviewRepository, OrderPurchasePort orderPurchasePort) {
        this.reviewRepository = reviewRepository;
        this.orderPurchasePort = orderPurchasePort;
    }

    public Review addReview(Long productId, String authorName, Integer rating, String comment) {
        orderPurchasePort.hasPurchaseForProduct(productId);
        Review review = new Review(productId, authorName == null ? "anonymous" : authorName, rating, comment);
        return reviewRepository.save(review);
    }

    public List<Review> listReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}
