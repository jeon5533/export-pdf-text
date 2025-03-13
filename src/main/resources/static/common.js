$(document).ready(function () {


    $(document).on('click', '.q-submit-btn', function (event) {


        event.preventDefault(); // 폼 제출 방지

        // 현재 버튼이 속한 폼 찾기
        var $form = $(this).closest("form");

        // 폼 내 모든 input 값 가져오기
        var formData = {
            qnum: $form.find(".qnum-input").val(),
            question: $form.find(".question").val(),
            option1: $form.find(".option-1").val(),
            option2: $form.find(".question-2").val(),
            option3: $form.find(".question-3").val(),
            option4: $form.find(".question-4").val(),
            option5: $form.find(".question-5").val()
        };

        console.log(
        `qnum : ${formData.qnum} \n question : ${formData.question} \n op1 : ${formData.option1} \n op2 : ${formData.option2} \n op3 : ${formData.option3} \n op4 : ${formData.option4} \n op5 : ${formData.option5}`
        );




    });













});