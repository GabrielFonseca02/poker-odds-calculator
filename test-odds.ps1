try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/odds" -Method Post -ContentType "application/json" -Body '{"heroCards":[{"rank":"ACE","suit":"HEARTS"}],"opponents":1,"simulations":10000}'
} catch {
    $_.ErrorDetails.Message
}