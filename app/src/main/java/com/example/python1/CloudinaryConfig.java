package com.example.python1;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class CloudinaryConfig {
    private static Cloudinary cloudinary;

    public static Cloudinary getCloudinaryInstance() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "dw02yjba1", // Your cloud name
                    "api_key", "654678513986681", // Your API key
                    "api_secret", "-UaXwGAkmSi4qr3MOO-57Lrb_Xw8" // Your API secret
            ));
        }
        return cloudinary;
    }
}

