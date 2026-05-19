This is the one API document I have.

However, the good news is 1. Hallandale is one of the largest and most important pharmacies for us and 2. They are built on Life File’s API, as are many/most of the pharmacies. Probably still need to be treated as separate integrations because I’m guessing there are differences, but I’m hoping that the API is the same for all of them. It probably is. 


Hello Josef,

The following are the requested sandbox credentials:

    Sandbox Pharmacy User:
        Full Login URL: https://host100.lifefile.net/apitest/pharmacy
        Username: CENSORED
        Password: CENSORED
        Order Statuses (code and name):
            Code: bc3f4, Name: 11437 Sandbox Status A
            Code: bb0d3, Name: 11437 Sandbox Status B
    Sandbox API Credentials:
        lifefile-sandbox.env has values for keys
        Keys:
            FullAPIBaseURL
            APIUsername
            APIPassword
            PracticeID
            PracticeName
            VendorID
            LocationID
            APINetworkID
            APINetworkName
    Available sandbox shipping services:
    | ID | Name |
    |9 |Pharmacy pickup |
    |999 |Delivery |
    |6223|Fedex 2 Day |
    |6224|Fedex Express Saver |
    |6225|Fedex Ground |
    |6226|Fedex First Overnight |
    |6227|Fedex Ground Home Delivery |
    |6228|Fedex Priority Overnight |
    |6230|Fedex Standard Overnight |
    |6231|Fedex 2 Day AM |


Available sandbox products: 
LF Product ID | Name | Strength | Form | Schedule Code | Quantity Units |
305157968 | Benzocaine, Lidocaine, Tetracaine Susp Dental | 10%, 10%, 4%. | Paste | L | grams |
305492218 | Baclofen, Dexamethasone, Flurbiprofen Emulsion | 2.5%,0.5%,5% | Cream | L | grams |
305492220 | Acarbose1 | 50mg | Tablet | L | each |
305492221 | Acetaminophen | 500mg | Tablet | O | each |
305492222 | Acyclovir | 5% | Ointment | L | Grams | 
Please refer to the latest LF API documentation attached. | 