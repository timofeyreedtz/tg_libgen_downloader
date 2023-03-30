package com.tg.libgen_downloader.service;

import com.tg.libgen_downloader.config.LibgenBotProps;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@Getter
@Setter
public class LibgenBotService {
    private LibgenBotProps libgenBotProps;
    private String mainUrl,bookHtmlPageUrl,downloadHtmlPageUrl,downloadUrl,file_catalog;
    @Autowired
    public LibgenBotService(LibgenBotProps libgenBotProps) {
        this.libgenBotProps = libgenBotProps;
        file_catalog = libgenBotProps.getFile_catalog();
    }

    public String getUrl(String book_name,String category) {
        String path = "";
        File file;
        switch (category){
            case "SCIENCE":
                mainUrl = libgenBotProps.getNon_science_url();
                bookHtmlPageUrl = libgenBotProps.getNon_science_first();
                downloadHtmlPageUrl = libgenBotProps.getNon_science_second();
                downloadUrl = libgenBotProps.getNon_science_and_fiction_final();
                try {
                    validateQuery(book_name);
                    Document doc = Jsoup.connect(mainUrl+ book_name).get();
                    Element getBookPage = doc.select("a[href]").stream()
                            .filter(x->x.attr("abs:href").startsWith(bookHtmlPageUrl))
                            .findFirst().orElseThrow(()-> new IllegalStateException("There is no such book."));
                    Element elementExtension = doc.select("td[nowrap]").stream()
                            .filter(x->x.text().equals("djvu")
                                    ||x.text().equals("pdf")
                                    ||x.text().equals("epub")
                                    ||x.text().equals("zip")
                                    ||x.text().equals("rar")
                            )
                            .findFirst().orElseThrow(()->new IllegalStateException("Unknown format of a book"));
                    String extension = "."+elementExtension.text();
                    String name = getBookPage.text().trim();
                    path = file_catalog+name+extension;
                    if(new File(path).exists()){
                        return path;
                    }
                    else{
                        doc = Jsoup.connect(getBookPage.attr("abs:href")).get();
                        Element getDownloadPage = doc.select("a[href]").stream()
                                .filter(x->x.attr("abs:href").startsWith(downloadHtmlPageUrl))
                                .findFirst().orElseThrow(()-> new IllegalStateException("There is no such book."));
                        path = getDownloadResponse(getDownloadPage,path);
                    }
                } catch (IllegalStateException  | IOException e) {
                    return e.getMessage();
                }
                break;
            case"FICTION":
                mainUrl = libgenBotProps.getFiction_url();
                downloadHtmlPageUrl = libgenBotProps.getFiction_first();
                downloadUrl = libgenBotProps.getNon_science_and_fiction_final();
                try {
                    validateQuery(book_name);
                    Document doc = Jsoup.connect(mainUrl+ book_name).get();
                    Element elementExtension = doc.select("td[title]").stream()
                            .filter(x->x.text().contains("EPUB")
                                    || x.text().contains("PDF")
                                    || x.text().contains("RAR")
                                    || x.text().contains("MOBI")
                                    || x.text().contains("MHT")
                                    || x.text().contains("RTF")
                                    || x.text().contains("TXT")
                                    || x.text().contains("HTML")
                                    || x.text().contains("DOCX"))
                            .findFirst().orElseThrow(()->new IllegalStateException("Something went wrong"));
                    String[] extensions = elementExtension.text().split(" ");
                    String extension = "."+extensions[0].toLowerCase();
                    Element title = doc.select("a[href]").stream()
                            .filter(x->x.attr("abs:href").length() == 58)
                            .findFirst().orElseThrow(()-> new IllegalStateException("There is no such book."));
                    String name = title.text().replace(".","").trim();
                    Element getBookPage = doc.select("a[href]").stream()
                            .filter(x->x.attr("abs:href").startsWith(downloadHtmlPageUrl))
                            .findFirst().orElseThrow(()-> new IllegalStateException("There is no such book."));
                    path = file_catalog+name+extension;
                    if(new File(path).exists()){
                        return path;
                    }
                    else{
                        path = getDownloadResponse(getBookPage,path);
                    }
                } catch (IllegalStateException  | IOException e) {
                    return e.getMessage();
                }
                break;
            case "SCIENCE_ARTICLE":
                mainUrl = libgenBotProps.getArticle_url();
                downloadHtmlPageUrl = libgenBotProps.getArticle_first();
                downloadUrl = libgenBotProps.getArticle_second();
                try {
                    validateQuery(book_name);
                    Document doc = Jsoup.connect(mainUrl+ book_name).get();
                    Element getArticlePage = doc.select("a").stream()
                            .filter(x->x.absUrl("href").startsWith(downloadHtmlPageUrl))
                            .findFirst().orElseThrow(()->new IllegalStateException("Something went wrong"));
                    Element title = doc.select("table").stream()
                            .findFirst().orElseThrow()
                            .getElementsByTag("tbody").stream()
                            .findFirst().orElseThrow()
                            .getElementsByTag("tr").stream()
                            .findFirst().orElseThrow()
                            .getElementsByTag("td")
                            .stream().skip(1).findFirst().orElseThrow()
                            .getElementsByTag("p").stream().findFirst().orElseThrow();
                    String name = title.text().trim();
                    if(name.length() >=255){
                        name = name.substring(0,100);
                    }
                    path = file_catalog+name+".pdf";
                    if(new File(path).exists()){
                        return path;
                    }
                    else{
                        path = getDownloadResponse(getArticlePage,path);
                    }
                } catch (IllegalStateException  | IOException e) {
                    return e.getMessage();
                }
                break;
        }
        return path;
    }

    private void validateQuery(String book_name) {
        if(book_name.length()<2){
            throw new IllegalStateException("Name of the book must contain at least 2 characters");
        }
    }

    private String download(String urlLink, File fileLoc) {
        try {
            byte[] buffer = new byte[1024];
            int readbyte = 0; //Stores the number of bytes written in each iteration.
            URL url = new URL(urlLink);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            double filesize = (double)http.getContentLengthLong();
            if(filesize >= 50000000){
                return "Big file:"+urlLink;
            }
            else {
                BufferedInputStream input = new BufferedInputStream(http.getInputStream());
                OutputStream ouputfile = new FileOutputStream(fileLoc);
                BufferedOutputStream bufferOut = new BufferedOutputStream(ouputfile, 1024);
                while((readbyte = input.read(buffer, 0, 1024)) >= 0) {
                    bufferOut.write(buffer,0,readbyte);
                }
                System.out.println("Your download is now complete.");
                bufferOut.close();
                input.close();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return "download complete";
    }
    private String getDownloadResponse(Element getPage, String path){
        Document doc = null;
        try {
            doc = Jsoup.connect(getPage.attr("abs:href")).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert doc != null;
        Element getDownloadUrl = doc.select("a[href]").stream()
                .filter(x->x.attr("abs:href").startsWith(downloadUrl))
                .findFirst().orElseThrow(()->new IllegalStateException("Something went wrong"));
        File file = new File(path);
        String response = download(getDownloadUrl.attr("abs:href"),file);
        if(response.equals("big file")){
            return "This file is too big. Try to download it by this url. If url doesn't work, try to turn vpn on."
                    +"\n"+getDownloadUrl.attr("abs:href");
        }
        else return path;
    }
}
