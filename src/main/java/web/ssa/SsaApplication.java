package web.ssa;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;  // 추가된 부분
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import web.ssa.entity.categories.Categories;
import web.ssa.enumf.CategoryType;
import web.ssa.repository.categories.CategoryRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SpringBootApplication
@EnableScheduling
public class SsaApplication {

    public static void main(String[] args) {

        SpringApplication.run(SsaApplication.class, args);

    }

    @Bean
    public CommandLineRunner initCategories(CategoryRepository catRepository) {

//        try{
//            String name = request.getParameter("name");
//            String query = "SELECT * FROM usrinfo WHERE name=? ";
//            PreparedStatement pstmt = con.prepareSatement(query);
//            executeQuery();
//            pstmt.setString(1, name);
//            ResultSet rs = pstmt.executeQuery();
//            ...
//        }catch (SQLException e){
//            ...
//        }finally {
//            ...
//        }
        

        return args -> {
            for (CategoryType type : CategoryType.values()) {
                if (catRepository.findByCode(type).isEmpty()) {
                    catRepository.save(Categories.builder()
                            .code(type)
                            .name(type.getDisplayName())
                            .build());
                }
            }
        };
    }

}
