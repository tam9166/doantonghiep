package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Post;

import java.util.Date;

public record PostResponse(
        Integer id,
        String title,
        String content,
        String image,
        String type,
        Integer likes,
        Boolean active,
        Date createDate) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getTitle(), post.getContent(), post.getImage(),
                post.getType(), post.getLikes(), post.getActive(), post.getCreateDate());
    }
}
