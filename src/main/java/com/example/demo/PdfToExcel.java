package com.example.demo;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class PdfToExcel {


    @GetMapping("/run")
    public String run(Model model) throws Exception{

        // 수능 국어 기출 문제 폴더 : 2015 ~ 2025
        String filePath = "C:\\Project\\exam\\2025_kor_odd.pdf";

        // 디렉토리
        File file = new File(filePath);


        String pdfText = "";

            // pdf
            PDDocument document = Loader.loadPDF(file);

            TextStripper fontExtractor = new TextStripper();
            fontExtractor.getText(document);

            // 가로 중앙 x좌표 (centerX)
            PDPage page = document.getPage(0);
            PDRectangle mediaBox = page.getMediaBox();
            float pageWidth = mediaBox.getWidth(); // 페이지의 가로 길이
            int centerX = (int)(pageWidth / 2);

            // font data -> 한 글자의 메타 데이터
            /*
             * {
             *   "page": 1,
             *   "text": "수",
             *   "size": 11,
             *   "width": 12,
             *   "x": 64.0283,
             *   "y": 1111.0283,
             * }
             * */
            List<Map<String, Object>> textList = fontExtractor.getFontData();

            // 1. 페이지 순으로 정렬
            // 2. 가로 길이의 절반으로 분리하여 좌 , 우로 정렬
            // 3. y좌표로 정렬
            // 4. x좌표로 정렬
            // 실제 눈에 보이는 것과 동일하게 정렬된 상태
            textList.sort(Comparator.comparing((Map<String, Object> m) -> (Integer) m.get("page"))
                    .thenComparing(m -> ((Integer) m.get("x") < centerX ? 0 : 1)) // 2. centerX 기준으로 왼쪽 먼저 정렬
                    .thenComparing(m -> (Integer) m.get("y")) // 3. 같은 그룹 내에서 y 오름차순 정렬
                    .thenComparing(m -> (Integer) m.get("x")));

            // 마지막 페이지 번호
            int totalPage = (int)textList.get(textList.size() - 1).get("page");

            // 각 요소별로 분리
            /*
            * 1. 공통지문
            * 2. 문제 + 문제 보기 (분리 불가능 , 수작업 필요)
            * 3. 선택지 1~5
            *
            * */
            for(int i = 0; i < textList.size(); i++){

                Map<String, Object> font = fontExtractor.getFontData().get(i);

                // 20년도 이후 시험부터는 선택과목이 생김. 별도 처리 필요.


                // 하단 페이지 정보, 저작권 정보 부분
                if((int)font.get("y") >= 1090 ){
                    continue;
                }
                // 상단 시험 정보 부분
                if((int)font.get("page") == 1 ){ // 첫 페이지는 더 큼. 20년도 이후 시험 부터는 첫 페이지 뿐만 아니라 선택과목 시작 부분도 200 으로 적용 되도록 처리 필요
                    if( (int)font.get("y") <= 200 ){
                        continue;
                    }
                }else{
                    if( (int)font.get("y") <= 160 ){
                        continue;
                    }
                }


                String ch = (String)font.get("text");

                // 각 요소의 시작과 끝 부분에 식별 가능한 식별자 추가
                if(i == 0){
                    pdfText += ch;
                }else{

                    /*
                    * 공통 지문 시작 끝 _sCp , /eCp - ok
                    * 문제 + 보기 시작 끝 _sQ , /eQ - ok
                    * 선택지 시작 끝 _1s , /1e - ok
                    */
                    switch (ch){

                        case "①": ch = "/eQ_1s"+ch; break;

                        case "②": ch = "/1e_2s"+ch; break;

                        case "③": ch = "/2e_3s"+ch; break;

                        case "④": ch = "/3e_4s"+ch; break;

                        case "⑤": ch = "/4e_5s"+ch; break;

                    }

                    // 이전 텍스트와의 높이 차이
                    int height = ( (int)font.get("y") - (int)fontExtractor.getFontData().get(i - 1).get("y") ) < 0 ? -1 * ( (int)font.get("y") - (int)fontExtractor.getFontData().get(i - 1).get("y") ) : (int)font.get("y") - (int)fontExtractor.getFontData().get(i - 1).get("y");

                    if(height == 0 || height == 18 || height == 19){
                        pdfText += ch;
                    }else{
                        pdfText += /*"-----"+height+*/"<br>"+ch;
                    }

                }

            }
            
            // 문제 구성 요소 시작 끝 표시
            pdfText = pdfText.replaceAll("\\[(\\d+)～(\\d+)\\]" , "_sCp[$1~$2]") // 공통 지문 시작
                    .replaceAll("(_sCp)(.*?)(<br>)(\\d+\\.)" , "$1$2/eCp_sepLine_<br>$4") // 공통 지문 끝
                    .replaceAll("(<br>)(\\d+\\.)" , "<br>_sQ$2") // 문제 시작
                    .replaceAll("(⑤)(.*?)(?=<br>)" , "$1$2/5e<br>_sepLine_") // 5번 선택지 종료
                    .replaceAll("<br>", "");


            // 공통 보기 List
            List<Map<String,String>> cpList = new ArrayList<>();
            Pattern cpPattern = Pattern.compile("_sCp(.*?)/eCp");
            Matcher matcher = cpPattern.matcher(pdfText);
            String reg = "\\[(\\d+)~(\\d+)\\]";
            while (matcher.find()) {

                String matchStr = matcher.group(1);

                Matcher mat = Pattern.compile(reg).matcher(matchStr);

                String nums =  mat.find() ? mat.group() : "";
                String content = matchStr.replaceAll(reg , "");

                Map<String,String> element = new HashMap<>();
                element.put("nums" , nums);
                element.put("content" , content);

                cpList.add(element);
            }


            // 문제 덩어리만 주줄 (문제 , 보기 , 선택지)
            List<String> compList = Arrays.asList(pdfText.split("_sepLine_"));
            List<String> qResList = new ArrayList<>();

            for(String comp : compList){
                if(comp.startsWith("_sQ")){
                    qResList.add(comp);
                }
            }

            // 문제 List
            List<LinkedHashMap<String,String>> qList = new ArrayList<>();
            String qReg = "(_sQ)(\\d+\\.)(.*?)(/eQ)";

            String qnReg = "(_sQ)(\\d+)(\\.)";

            String op1Reg = "(_1s①)(.*?)(/1e)";
            String op2Reg = "(_2s②)(.*?)(/2e)";
            String op3Reg = "(_3s③)(.*?)(/3e)";
            String op4Reg = "(_4s④)(.*?)(/4e)";
            String op5Reg = "(_5s⑤)(.*?)(/5e)";

            for(String qStr : qResList){
                LinkedHashMap<String,String> element = new LinkedHashMap<>();

                Matcher qmat = Pattern.compile(qReg).matcher(qStr);
                String question =  qmat.find() ? qmat.group(3) : "";

                Matcher qnmat = Pattern.compile(qnReg).matcher(qStr);
                String qnum = qnmat.find() ? qnmat.group(2) : "";

                Matcher op1mat = Pattern.compile(op1Reg).matcher(qStr);
                String op1 = op1mat.find() ? op1mat.group(2) : "";

                Matcher op2mat = Pattern.compile(op2Reg).matcher(qStr);
                String op2 = op2mat.find() ? op2mat.group(2) : "";

                Matcher op3mat = Pattern.compile(op3Reg).matcher(qStr);
                String op3 = op3mat.find() ? op3mat.group(2) : "";

                Matcher op4mat = Pattern.compile(op4Reg).matcher(qStr);
                String op4 = op4mat.find() ? op4mat.group(2) : "";

                Matcher op5mat = Pattern.compile(op5Reg).matcher(qStr);
                String op5 = op5mat.find() ? op5mat.group(2) : "";


                element.put("qnum" , qnum);
                element.put("question" , question);
                element.put("op1" , op1);
                element.put("op2" , op2);
                element.put("op3" , op3);
                element.put("op4" , op4);
                element.put("op5" , op5);
                qList.add(element);

            }

            /*
             * 공통 지문 시작 끝 _sCp , /eCp - ok
             * 문제 + 보기 시작 끝 _sQ , /eQ - ok
             * 선택지 시작 끝 _1s , /1e - ok (5번 처리 필요)
             */
            /*
            pdfText = pdfText.replaceAll("(_sCp)(.*?)(/eCp)" , "<textarea class=\"t-ar\">$2</textarea><div class=\"line\"></div>")
                            .replaceAll("(_sQ)(\\d+)(.)(.*?)(/eQ)" , "<input type=\"text\" class=\"q-ar\" data-qtionno=\"$2\" value=\"$4\"/>")
                            .replaceAll("(_1s)(.*?)(/1e)","<input type=\"text\" class=\"op-ar\" data-optionno=\"1\" value=\"$2\"/>")
                            .replaceAll("(_2s)(.*?)(/2e)","<input type=\"text\" class=\"op-ar\" data-optionno=\"2\" value=\"$2\"/>")
                            .replaceAll("(_3s)(.*?)(/3e)","<input type=\"text\" class=\"op-ar\" data-optionno=\"3\" value=\"$2\"/>")
                            .replaceAll("(_4s)(.*?)(/4e)","<input type=\"text\" class=\"op-ar\" data-optionno=\"4\" value=\"$2\"/>")
                            .replaceAll("(_5s)(.*?)(/5e)","<input type=\"text\" class=\"op-ar\" data-optionno=\"5\" value=\"$2\"/><div class=\"line\"></div>");
            */
            // System.out.println(pdfText);
            // pdfText = "";

        // model.addAttribute("content" , "<div style=\"display: flex; flex-direction: column;\">"+pdfText+"</div>");

        model.addAttribute("cpList" , cpList);
        model.addAttribute("qList" , qList);
        return "/form.html";


    }


}
