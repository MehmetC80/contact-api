package de.memozone.contactapi.service;

import de.memozone.contactapi.domain.Contact;
import de.memozone.contactapi.repository.ContactRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static de.memozone.contactapi.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService{


    private  final ContactRepository contactRepository;


    @Override
    public Page<Contact> getAllContact(int page, int size) {
        return contactRepository.findAll(PageRequest.of(page,size, Sort.by("name")));
    }

    @Override
    public Contact getContact(String id) {
        return contactRepository.findById(id).orElseThrow(()-> new RuntimeException("Contact not found"));
    }

    @Override
    public Contact createContact(Contact contact) {
        return contactRepository.save(contact);
    }

    @Override
    public void deleteContact(String id) {
        contactRepository.deleteById(id);
    }

    public String uploadPhoto(String id, MultipartFile file){
        Contact contact = getContact(id);
        String photoUrl=photoFunction.apply(id,file);
        contact.setPhotoUrl(photoUrl);
        contactRepository.save(contact);
        return photoUrl;
    }


    private final Function<String,String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map( name -> "." + name.substring(filename.lastIndexOf(".")+1)).orElse(".png");

    private final BiFunction<String,MultipartFile,String> photoFunction=(id,image) ->{
        String filename = id+ fileExtension.apply(image.getOriginalFilename());
        try{

            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if(!Files.exists(fileStorageLocation)){
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(),fileStorageLocation.resolve(filename),REPLACE_EXISTING);
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/contacts/image/"+filename).toUriString();
        }catch(Exception exception){
            throw new RuntimeException("Unable to save image!");
        }
    };
}
