import React, {useEffect, useState} from 'react';
import {useNavigate, useParams} from "react-router-dom";
import {Alert} from "react-bootstrap";

import RecipeForm from "../RecipeForm/RecipeForm";
import {fetchRecipe, updateRecipe} from "../../apis/recipeApi";

function EditRecipe() {
    const {recipeId} = useParams()
    const navigate = useNavigate()

    const [recipe, setRecipe] = useState(null)
    const [loadError, setLoadError] = useState(null)

    useEffect(() => {
        fetchRecipe(recipeId)
            .then(setRecipe)
            .catch(() => setLoadError("The recipe could not be loaded."))
    }, [recipeId])

    if (loadError) {
        return <Alert variant="danger" className="m-3">{loadError}</Alert>
    }

    // The form is only rendered once the recipe is there, so it starts with the stored values
    if (!recipe) {
        return null
    }

    return (
        <RecipeForm
            title="Edit Recipe"
            submitLabel="Save"
            initialRecipe={recipe}
            onSubmit={(changedRecipe) => updateRecipe(recipeId, changedRecipe).then(() => navigate("/"))}
        />
    )
}

export default EditRecipe;
