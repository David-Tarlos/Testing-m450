import React from 'react';
import './AddIngredient.css';
import {Button, Col, Form, Row} from "react-bootstrap";

// Muss mit dem Enum ch.tbz.recipe.planner.domain.Unit im Backend uebereinstimmen
const UNITS = ['PIECE', 'GRAMM', 'KILOGRAMM', 'LITRE', 'DECILITRE'];

const AddIngredient = ({ingredient, updateIngredient, removeIngredient}) => {

    const handleChange = (field) => (e) => updateIngredient({...ingredient, [field]: e.target.value})

    return (
        <Row>
            <Col>
                <Form.Group className="mb-1">
                    <Form.Control
                        placeholder="Name"
                        aria-label="Ingredient name"
                        required
                        value={ingredient.name}
                        onChange={handleChange('name')}
                    />
                </Form.Group>
            </Col>
            <Col>
                <Form.Group className="mb-1">
                    <Form.Select aria-label="Unit" value={ingredient.unit} onChange={handleChange('unit')}>
                        {UNITS.map(unit => <option key={unit}>{unit}</option>)}
                    </Form.Select>
                </Form.Group>
            </Col>
            <Col>
                <Form.Group className="mb-1">
                    <Form.Control
                        type="number"
                        min="1"
                        step="1"
                        placeholder="Quantity"
                        aria-label="Quantity"
                        required
                        value={ingredient.amount}
                        onChange={handleChange('amount')}
                    />
                </Form.Group>
            </Col>
            <Col xs={1}>
                <Button
                    onClick={e => removeIngredient(ingredient)}
                    variant='outline-dark'
                    className="mb-1"
                >x</Button>
            </Col>
        </Row>
    )
}

AddIngredient.propTypes = {};

AddIngredient.defaultProps = {};

export default AddIngredient;
